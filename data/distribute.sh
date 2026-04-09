#!/bin/bash
# ============================================================================
# distribute.sh - Distribui os CSVs de dados do exercicio 04 para todas as
#                 pastas data/ necessarias (4 linguagens x 2 fases)
#
# Uso:  ./distribute.sh
#
# O arquivo transactions_1m.csv.gz e descompactado automaticamente.
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

DESTINATIONS=(
    "04-batch-performance/manual/bun/data"
    "04-batch-performance/manual/java/data"
    "04-batch-performance/manual/kotlin/data"
    "04-batch-performance/manual/php/data"
    "04-batch-performance/ai/bun/data"
    "04-batch-performance/ai/java/data"
    "04-batch-performance/ai/kotlin/data"
    "04-batch-performance/ai/php/data"
)

CSV_FILES=(
    "transactions_1k.csv"
    "transactions_10k.csv"
    "transactions_50k.csv"
    "transactions_100k.csv"
    "transactions_500k.csv"
)

echo -e "${CYAN}Distribuindo datasets para o exercicio 04...${NC}"
echo ""

# Descompactar o 1m se ainda nao estiver descompactado
if [ ! -f "$SCRIPT_DIR/transactions_1m.csv" ]; then
    if [ -f "$SCRIPT_DIR/transactions_1m.csv.gz" ]; then
        echo -e "${YELLOW}Descompactando transactions_1m.csv.gz (~19MB -> ~53MB)...${NC}"
        gunzip -k "$SCRIPT_DIR/transactions_1m.csv.gz"
        echo -e "${GREEN}Descompactado.${NC}"
        echo ""
    else
        echo -e "${RED}ERRO: transactions_1m.csv.gz nao encontrado em $SCRIPT_DIR${NC}"
        exit 1
    fi
fi

CSV_FILES+=("transactions_1m.csv")

# Copiar para todos os destinos
for dest in "${DESTINATIONS[@]}"; do
    full_dest="$REPO_ROOT/$dest"
    if [ ! -d "$full_dest" ]; then
        echo -e "${YELLOW}Criando $dest${NC}"
        mkdir -p "$full_dest"
    fi

    for csv in "${CSV_FILES[@]}"; do
        src="$SCRIPT_DIR/$csv"
        if [ -f "$src" ]; then
            cp "$src" "$full_dest/$csv"
        fi
    done
    echo -e "  ${GREEN}✓${NC} $dest"
done

echo ""
echo -e "${GREEN}Distribuicao concluida!${NC}"
echo -e "${CYAN}Arquivos copiados:${NC} ${#CSV_FILES[@]} CSVs x ${#DESTINATIONS[@]} pastas"

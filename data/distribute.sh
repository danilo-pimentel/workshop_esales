#!/bin/bash
# ============================================================================
# distribute.sh - Distribui os CSVs de dados do exercicio 04 para as pastas
#                 data/ das linguagens escolhidas
#
# Uso:
#   ./distribute.sh                      (abre menu interativo)
#   ./distribute.sh all                  (copia para todas as 8 pastas)
#   ./distribute.sh manual               (copia para todas as pastas manual/)
#   ./distribute.sh ai                   (copia para todas as pastas ai/)
#   ./distribute.sh manual bun           (copia so para manual/bun)
#   ./distribute.sh ai kotlin            (copia so para ai/kotlin)
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
BOLD='\033[1m'
NC='\033[0m'

CSV_FILES=(
    "transactions_1k.csv"
    "transactions_10k.csv"
    "transactions_50k.csv"
    "transactions_100k.csv"
    "transactions_500k.csv"
    "transactions_1m.csv"
)

# ── Descompactar o 1m se necessario ─────────────────────────────────────────
ensure_1m_extracted() {
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
}

# ── Copiar para um destino especifico ───────────────────────────────────────
copy_to() {
    local phase="$1"
    local lang="$2"
    local dest="$REPO_ROOT/04-batch-performance/$phase/$lang/data"

    if [ ! -d "$REPO_ROOT/04-batch-performance/$phase/$lang" ]; then
        echo -e "  ${RED}✗${NC} $phase/$lang — pasta do exercicio nao encontrada"
        return 1
    fi

    mkdir -p "$dest"
    for csv in "${CSV_FILES[@]}"; do
        if [ -f "$SCRIPT_DIR/$csv" ]; then
            cp "$SCRIPT_DIR/$csv" "$dest/$csv"
        fi
    done
    echo -e "  ${GREEN}✓${NC} $phase/$lang"
}

# ── Copiar para todas as linguagens de uma fase ─────────────────────────────
copy_phase() {
    local phase="$1"
    echo -e "${YELLOW}Copiando para ${BOLD}$phase${NC}${YELLOW}/...${NC}"
    for lang in bun java kotlin php; do
        copy_to "$phase" "$lang"
    done
}

# ── Copiar para todos ────────────────────────────────────────────────────────
copy_all() {
    echo -e "${YELLOW}Copiando para TODAS as 8 pastas...${NC}"
    for phase in manual ai; do
        for lang in bun java kotlin php; do
            copy_to "$phase" "$lang"
        done
    done
}

# ── Menu interativo ─────────────────────────────────────────────────────────
show_menu() {
    echo ""
    echo -e "${BOLD}============================================================${NC}"
    echo -e "${BOLD}  Distribute - Datasets do Exercicio 04${NC}"
    echo -e "${BOLD}============================================================${NC}"
    echo ""
    echo -e "  ${BOLD}Fase Manual:${NC}"
    echo -e "    ${CYAN}1)${NC}  manual/bun"
    echo -e "    ${CYAN}2)${NC}  manual/java"
    echo -e "    ${CYAN}3)${NC}  manual/kotlin"
    echo -e "    ${CYAN}4)${NC}  manual/php"
    echo -e "    ${CYAN}5)${NC}  TODAS as linguagens de manual/"
    echo ""
    echo -e "  ${BOLD}Fase AI:${NC}"
    echo -e "    ${CYAN}6)${NC}  ai/bun"
    echo -e "    ${CYAN}7)${NC}  ai/java"
    echo -e "    ${CYAN}8)${NC}  ai/kotlin"
    echo -e "    ${CYAN}9)${NC}  ai/php"
    echo -e "    ${CYAN}10)${NC} TODAS as linguagens de ai/"
    echo ""
    echo -e "  ${YELLOW}A)${NC}  TODAS as pastas (manual + ai, 8 destinos)"
    echo ""
    echo -e "  ${RED}0)${NC}  Sair"
    echo ""
    echo -e "${BOLD}============================================================${NC}"
    echo -n "  Escolha uma opcao: "
}

# ── Main ─────────────────────────────────────────────────────────────────────

ARG1="${1:-}"
ARG2="${2:-}"

# Modo nao-interativo
if [ -n "$ARG1" ]; then
    ensure_1m_extracted
    case "$ARG1" in
        all|tudo)
            copy_all
            ;;
        manual|ai)
            if [ -n "$ARG2" ]; then
                case "$ARG2" in
                    bun|java|kotlin|php)
                        echo -e "${YELLOW}Copiando para $ARG1/$ARG2...${NC}"
                        copy_to "$ARG1" "$ARG2"
                        ;;
                    *)
                        echo -e "${RED}Linguagem invalida: $ARG2 (use bun, java, kotlin ou php)${NC}"
                        exit 1
                        ;;
                esac
            else
                copy_phase "$ARG1"
            fi
            ;;
        *)
            echo -e "${RED}ERRO: argumento invalido '$ARG1'${NC}"
            echo "Use: all, manual, ai, 'manual <lang>', 'ai <lang>' ou sem argumentos para o menu"
            exit 1
            ;;
    esac
    echo ""
    echo -e "${GREEN}Distribuicao concluida!${NC}"
    exit 0
fi

# Modo interativo
show_menu
read -r OPCAO

ensure_1m_extracted

case "$OPCAO" in
    1)  echo -e "${YELLOW}Copiando para manual/bun...${NC}";    copy_to "manual" "bun" ;;
    2)  echo -e "${YELLOW}Copiando para manual/java...${NC}";   copy_to "manual" "java" ;;
    3)  echo -e "${YELLOW}Copiando para manual/kotlin...${NC}"; copy_to "manual" "kotlin" ;;
    4)  echo -e "${YELLOW}Copiando para manual/php...${NC}";    copy_to "manual" "php" ;;
    5)  copy_phase "manual" ;;
    6)  echo -e "${YELLOW}Copiando para ai/bun...${NC}";        copy_to "ai" "bun" ;;
    7)  echo -e "${YELLOW}Copiando para ai/java...${NC}";       copy_to "ai" "java" ;;
    8)  echo -e "${YELLOW}Copiando para ai/kotlin...${NC}";     copy_to "ai" "kotlin" ;;
    9)  echo -e "${YELLOW}Copiando para ai/php...${NC}";        copy_to "ai" "php" ;;
    10) copy_phase "ai" ;;
    [aA]) copy_all ;;
    0)  echo "Saindo."; exit 0 ;;
    *)  echo -e "${RED}Opcao invalida.${NC}"; exit 1 ;;
esac

echo ""
echo -e "${GREEN}Distribuicao concluida!${NC}"

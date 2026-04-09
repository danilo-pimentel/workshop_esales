# Exercicio 04 - Processador de Lotes Financeiros (Performance)

## Contexto

Voce recebeu um sistema de processamento batch de transacoes financeiras. Ele recebe um CSV com milhares de transacoes entre contas bancarias e executa 5 etapas de processamento em sequencia.

O codigo esta **100% correto** - produz os resultados esperados. O problema e **performance**: com 10.000 registros ja e lento, e com 50.000 e impraticavel.

---

## Os dados

Cada transacao no CSV tem este formato:

```
id, amount, source_account, dest_account, timestamp, category, priority
1,  8526.13, ACC0670,        ACC0175,      1720719320005, REFUND,   4
2,  2506.74, ACC0883,        ACC0746,      1713775324658, TRANSFER, 3
3,  48.39,   ACC0471,        ACC0838,      1705686559902, WITHDRAWAL, 1
```

| Campo | Descricao |
|-------|-----------|
| `id` | Identificador unico da transacao |
| `amount` | Valor em reais (ex: 8526.13) |
| `source_account` | Conta de origem (ex: ACC0670) |
| `dest_account` | Conta de destino (ex: ACC0175) |
| `timestamp` | Data/hora em milissegundos Unix |
| `category` | Tipo: PAYMENT, TRANSFER, REFUND, PURCHASE, WITHDRAWAL, DEPOSIT, FEE, INTEREST |
| `priority` | 1 (baixa) a 5 (alta) |

Cada conta comeca com um saldo inicial de **R$ 10.000,00**.

---

## As 5 etapas de processamento

### Etapa 1: Deteccao de duplicatas

Transacoes sao consideradas **duplicatas** quando:
- Mesma conta de origem (`source_account`)
- Mesma conta de destino (`dest_account`)
- Mesmo valor (`amount`)
- Timestamps com diferenca menor que 5 segundos

**Exemplo:**
```
TX #100: ACC0050 -> ACC0200, R$ 500.00, timestamp: 1720000001000
TX #101: ACC0050 -> ACC0200, R$ 500.00, timestamp: 1720000003000  <-- DUPLICATA (2s de diferenca)
TX #102: ACC0050 -> ACC0200, R$ 500.00, timestamp: 1720000008000  <-- NAO e duplicata (7s de diferenca)
```

Duplicatas sao removidas. Apenas a primeira ocorrencia e mantida.

**O problema de performance:** Para cada nova transacao, o codigo percorre uma LinkedList de TODAS as transacoes ja vistas para verificar se e duplicata. Com 10.000 transacoes, isso significa ate 10.000 x 10.000 = 100 milhoes de comparacoes.

---

### Etapa 2: Validacao de saldos

Cada transacao debita a conta de origem e credita a conta de destino. Se a conta de origem nao tem saldo suficiente, a transacao e **rejeitada**.

**Exemplo:**
```
Saldo inicial de todas as contas: R$ 10.000,00

TX #1: ACC0050 -> ACC0200, R$ 3.000,00
  ACC0050: 10.000 - 3.000 = R$ 7.000,00 (OK)
  ACC0200: 10.000 + 3.000 = R$ 13.000,00

TX #2: ACC0050 -> ACC0300, R$ 8.000,00
  ACC0050 tem R$ 7.000, precisa de R$ 8.000 -> REJEITADA (saldo insuficiente)

TX #3: ACC0050 -> ACC0300, R$ 5.000,00
  ACC0050: 7.000 - 5.000 = R$ 2.000,00 (OK)
```

**O problema de performance:** Os saldos sao armazenados em uma LinkedList ordenada. Para cada transacao, o codigo faz uma busca linear na lista inteira para encontrar o saldo da conta de origem, depois outra busca para a conta de destino, e mais buscas para atualizar ambos. Com 1.000 contas, cada operacao percorre centenas de nos.

---

### Etapa 3: Deteccao de aneis de fraude

Um **anel de fraude** (fraud ring) e uma transferencia circular entre contas. O dinheiro sai de A, passa por varias contas intermediarias, e volta para A. Isso e um indicador classico de lavagem de dinheiro.

**Exemplo simples (3 contas):**
```
ACC0050 -> ACC0200  (TX #10)
ACC0200 -> ACC0300  (TX #25)
ACC0300 -> ACC0050  (TX #41)  <-- Fechou o ciclo! Anel de fraude detectado.
```

Visualmente:
```
ACC0050 --$--> ACC0200
  ^                |
  |                v
  +----$--- ACC0300
```

**Exemplo com anel maior (4 contas):**
```
ACC0001 -> ACC0002
ACC0002 -> ACC0003
ACC0003 -> ACC0004
ACC0004 -> ACC0001  <-- Anel de 4 contas
```

O sistema monta um **grafo direcionado** onde cada conta e um no e cada transacao e uma aresta. Depois, busca **ciclos** nesse grafo usando DFS (busca em profundidade).

**O problema de performance:** O grafo e representado como uma **matriz de adjacencia** (V x V), onde V e o numero de contas. Com 1.000 contas, a matriz tem 1.000.000 celulas. Alem disso, para encontrar os vizinhos de um no, o codigo varre uma linha inteira da matriz (1.000 celulas), mesmo que o no so tenha 2 vizinhos. Com grafos esparsos (poucas arestas), isso e extremamente desperdicador.

---

### Etapa 4: Ordenacao por prioridade

As transacoes aceitas precisam ser processadas por ordem de prioridade (5 = mais urgente, 1 = menos urgente).

**Exemplo:**
```
Antes:   TX#1(prio=3), TX#2(prio=5), TX#3(prio=1), TX#4(prio=5), TX#5(prio=2)
Depois:  TX#2(prio=5), TX#4(prio=5), TX#1(prio=3), TX#5(prio=2), TX#3(prio=1)
```

**O problema de performance:** Em vez de usar um sort nativo (O(n log n)), o codigo insere cada transacao uma a uma em uma LinkedList na posicao correta (insertion sort). Para cada insercao, percorre a lista desde o inicio ate encontrar a posicao. Isso e O(n) por insercao, totalizando O(n^2).

---

### Etapa 5: Relatorio por categoria

Agrupa as transacoes por categoria e calcula estatisticas:

**Exemplo de saida:**
```
Categoria   | Qtd  | Total         | Media
PAYMENT     | 1250 | R$ 6.234.000  | R$ 4.987,20
TRANSFER    | 980  | R$ 4.100.000  | R$ 4.183,67
REFUND      | 420  | R$ 1.890.000  | R$ 4.500,00
...
```

**O problema de performance:** Para cada categoria (8 tipos), o codigo faz um `filter()` percorrendo TODAS as transacoes. Com 8 categorias e 10.000 transacoes, sao 80.000 iteracoes - em vez de uma unica passada com acumuladores.

---

## Dinamica: 2 Fases

Este exercicio sera executado **duas vezes**, com abordagens diferentes:

### Fase 1: Otimizacao Manual (`manual/`)

Analise o codigo, identifique os gargalos e otimize **sem usar nenhuma ferramenta de AI**.

> **Objetivo:** Vivenciar a dificuldade de identificar e corrigir problemas de performance em codigo alheio.

### Fase 2: Otimizacao com AI (`ai/`)

Otimize os mesmos gargalos **usando ferramentas de AI** (Claude Code, Copilot, ChatGPT, etc).

> **Objetivo:** Comparar velocidade e qualidade das otimizacoes com vs sem AI.

**O codigo nas duas pastas e identico.** A unica diferenca e a abordagem usada.

---

## Dados de teste

Os datasets ja estao prontos na pasta `data/` de cada linguagem:

| Arquivo | Registros | Uso |
|---------|-----------|-----|
| `transactions_1k.csv` | 1.000 | Testes de corretude |
| `transactions_10k.csv` | 10.000 | Benchmark inicial (lento) |
| `transactions_50k.csv` | 50.000 | Teste de stress |
| `transactions_1m.csv` | 1.000.000 | Stress test pesado |

**Nao e necessario gerar dados.** Os CSVs sao identicos em todas as linguagens.

---

## Como executar

**Importante:** Navegue ate a pasta da fase antes de escolher a linguagem:
```bash
# Fase 1 (manual)
cd manual/bun    # ou manual/java, manual/kotlin, manual/php

# Fase 2 (AI)
cd ai/bun        # ou ai/java, ai/kotlin, ai/php
```

### Passo 1: Rode os testes de corretude
```bash
bun install && bun test                  # Bun
mvn test                                  # Java
./gradlew test                            # Kotlin (Linux)  |  .\gradlew test (Windows)
composer install && ./vendor/bin/phpunit  # PHP
```
**Resultado esperado:** 26 testes passando.

### Passo 2: Execute com 10k registros e observe a lentidao
```bash
bun run src/index.ts data/transactions_10k.csv         # Bun
mvn exec:java -Dexec.args="data/transactions_10k.csv"  # Java
./gradlew run --args="data/transactions_10k.csv"        # Kotlin
php src/index.php data/transactions_10k.csv             # PHP
```

### Passo 3: Otimize e teste com 50k
```bash
bun run src/index.ts data/transactions_50k.csv         # Bun
mvn exec:java -Dexec.args="data/transactions_50k.csv"  # Java
./gradlew run --args="data/transactions_50k.csv"        # Kotlin
php src/index.php data/transactions_50k.csv             # PHP
```

### Passo 4 (bonus): Teste com 1 milhao de registros
```bash
bun run src/index.ts data/transactions_1m.csv         # Bun
mvn exec:java -Dexec.args="data/transactions_1m.csv"  # Java
./gradlew run --args="data/transactions_1m.csv"        # Kotlin
php src/index.php data/transactions_1m.csv             # PHP
```

## Regras

- **NAO** altere as regras de negocio
- **NAO** remova nenhuma operacao (dedup, saldo, fraude, prioridade, relatorio)
- Apenas otimize **estruturas de dados e algoritmos**
- O resultado deve ser **identico** ao da versao original
- Todos os testes devem continuar passando

## Criterios de aceite

- Todos os 26 testes de corretude passando
- Processamento de 50k em tempo razoavel (< 5 segundos)
- Processamento de 1M em tempo razoavel (< 30 segundos) — bonus
- Nenhuma regra de negocio alterada

## Tempo estimado

| Fase | Tempo |
|------|-------|
| Manual (sem AI) | 30-45 min (provavelmente nao termina) |
| Com AI | 10-15 min |

Anote o tempo gasto em cada fase para comparacao!

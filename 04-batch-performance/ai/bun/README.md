# Ex.04 - Processador de Lotes Financeiros (Bun)

## Contexto

Sistema de processamento batch de transações financeiras. O código está correto
mas a performance é inadequada para volumes grandes.

## O desafio

1. Execute com 10.000 registros: `bun run src/index.ts data/transactions_10k.csv` (~20s)
2. Gere 1.000.000 registros: `bun run scripts/generate-data.ts 1000000 > data/transactions_1m.csv`
3. Execute com 1M: `bun run src/index.ts data/transactions_1m.csv` (inviável — horas)
4. **Otimize o código** para processar 1M em menos de 15 segundos
5. O resultado deve ser idêntico ao original

## Regras

- NÃO altere as regras de negócio
- Apenas otimize estruturas de dados e algoritmos
- Todos os testes devem continuar passando: `bun test`

## Pré-requisitos

- Bun 1.1+

## Como executar

```bash
bun install
bun run scripts/generate-data.ts 10000 > data/transactions_10k.csv
bun run src/index.ts data/transactions_10k.csv
bun test
```

## Os 5 gargalos intencionais

| # | Método | Complexidade atual | Sugestão de otimização |
|---|--------|--------------------|------------------------|
| 1 | `isDuplicate()` | O(n²) | Map com chave composta + janela temporal |
| 2 | `updateBalance()` | O(n²) | `Map<string, number>` |
| 3 | `detectFraudRings()` | O(V² + V×E) | Lista de adjacência + Tarjan/Kosaraju |
| 4 | `insertByPriority()` | O(n²) | Array.sort por batch |
| 5 | `generateReport()` | O(categories × n) | Acumuladores em um único O(n) pass |

## Estrutura do projeto

```
bun/
├── package.json
├── tsconfig.json
├── src/
│   ├── index.ts              # CLI entry: reads CSV, processes, outputs result
│   ├── processor.ts          # BatchProcessor com os 5 gargalos
│   ├── models.ts             # Transaction type/interface + CSV parsing
│   └── data-structures.ts    # LinkedList, AdjacencyMatrix (estruturas lentas)
├── scripts/
│   └── generate-data.ts      # Gera CSVs de qualquer tamanho
├── data/
│   └── .gitkeep
├── test/
│   ├── correctness.test.ts   # Valida resultados corretos
│   └── performance.test.ts   # Benchmarks: mede o tempo
└── README.md
```

## Formato do CSV

```
id,amount,source_account,dest_account,timestamp,category,priority
1,150.00,ACC001,ACC002,2024-01-15T10:30:00,transferencia,3
```

**Campos:**
- `id` — identificador único inteiro
- `amount` — valor em BRL (float com 2 casas decimais)
- `source_account` / `dest_account` — ACC001…ACC1000
- `timestamp` — ISO 8601 sem timezone (assume UTC)
- `category` — transferencia | pagamento | deposito | saque | investimento | emprestimo | seguro | taxa
- `priority` — 1 (menor) a 5 (maior)

## Regras de negócio

- **Saldo inicial:** R$ 50.000,00 por conta
- **Saldo negativo:** transações rejeitadas se o saldo da conta origem ficaria negativo
- **Duplicata:** mesmo source, dest, amount com timestamps a menos de 5 segundos
- **Priority:** dentro de cada janela de 100 transações, processa maior priority primeiro
- **Fraud ring:** ciclo no grafo de transferências (A→B→C→A)

# Financial Transaction Batch Processor — PHP 8.3

> **Optimization Challenge**: a deliberately slow batch processor riddled with
> classic algorithmic anti-patterns.  Your mission is to make 10 000
> transactions run in under 15 seconds (ideally under 2 seconds).

---

## The Challenge

| Batch size | Bottleneck build | Optimized target |
|------------|-----------------|-----------------|
| 1 000      | ~2 s            | < 0.5 s         |
| 10 000     | ~20 s           | < 2 s           |
| 100 000    | ~35 min         | < 15 s          |
| 1 000 000  | practically impossible | < 60 s   |

---

## Prerequisites

- **PHP 8.3** or newer (`php --version`)
- **Composer** (`composer --version`)

---

## Setup

```bash
cd exercicios/04-batch-performance/php     # Linux / macOS
cd exercicios\04-batch-performance\php     # Windows (PowerShell)
composer install
```

---

## Generating Test Data

```bash
# 10 000 transactions
php src/DataGenerator.php 10000 > data/transactions_10k.csv

# 100 000 transactions (for post-optimization testing)
php src/DataGenerator.php 100000 > data/transactions_100k.csv

# 1 000 000 transactions (only after optimization)
php src/DataGenerator.php 1000000 > data/transactions_1m.csv
```

---

## Running the Processor

```bash
php src/index.php data/transactions_10k.csv
```

Expected output (before optimization):

```
Loading transactions from: data/transactions_10k.csv
Loaded 10000 transactions.

Starting batch processing…
(This intentionally runs slow – see README.md for the optimization challenge.)

============================================================
BATCH PROCESSING RESULT SUMMARY
============================================================
Total transactions processed : 10000
Duplicates detected          : ~200
Fraud flags                  : ~8500
Processing time              : 20.xxxx s
...
============================================================
```

---

## Running the Tests

```bash
# Linux / macOS:
./vendor/bin/phpunit --testsuite Correctness
./vendor/bin/phpunit --testsuite Performance
./vendor/bin/phpunit
PHPUNIT_SKIP_SLOW=0 ./vendor/bin/phpunit --group slow

# Windows (PowerShell):
.\vendor\bin\phpunit --testsuite Correctness
.\vendor\bin\phpunit --testsuite Performance
.\vendor\bin\phpunit
$env:PHPUNIT_SKIP_SLOW="0"; .\vendor\bin\phpunit --group slow
```

**Suites disponíveis:**
- `Correctness` — Business logic assertions (fast, ~100 transactions)
- `Performance` — Timing benchmarks (logs times, no hard failures except extreme outliers)
- `slow` group — Run the slow 10 000-transaction benchmark

---

## The Five Bottlenecks

All bottlenecks are in `src/BatchProcessor.php`.

### Bottleneck 1 — Duplicate Detection `O(n²)`

**Location**: `BatchProcessor::detectDuplicates()`

For every transaction `T`, the method calls `LinkedList::find()` which performs
a full linear scan of all previously seen transactions.

```
n transactions × O(n) scan = O(n²)
```

**Fix hint**: use a hash map keyed by `"source|dest|amount"`, then a sorted
time-window scan on the (small) matching bucket.

---

### Bottleneck 2 — Account Balance Computation `O(n²)`

**Location**: `BatchProcessor::computeAccountBalances()`

A sorted `LinkedList` of `(account, balance)` pairs is maintained.  For every
transaction, the processor:

1. Calls `find()` to locate the account — `O(n)`
2. If found, rebuilds the entire list with the updated balance — `O(n)`
3. Calls `insertSorted()` to re-insert — `O(n)`

Total per transaction: `O(n)`.  For `n` transactions: `O(n²)`.

**Fix hint**: a plain PHP associative array (`$balances[$account] += $delta`)
gives `O(1)` per update.

---

### Bottleneck 3 — Fraud Ring Detection `O(V² + V×E)`

**Location**: `BatchProcessor::detectFraudRings()` and `BatchProcessor::dfs()`

The flow graph is an `AdjacencyMatrix`.  DFS is launched from **every** vertex.

- `getNeighbors()` on an adjacency matrix costs `O(V)` (scans the entire row)
  even when a vertex has few neighbours.
- Running DFS from every vertex: `O(V)` starts × `O(V²)` per DFS = `O(V³)`.

With ~2n unique accounts (`V ≈ 2n`), this is `O(n³)` in the worst case.

**Fix hint**: switch to an adjacency list (`array<string, string[]>`).
Use Tarjan's SCC algorithm for `O(V + E)` cycle detection.

---

### Bottleneck 4 — Priority Sort `O(n²)`

**Location**: `BatchProcessor::sortByPriority()`

Each of `n` transactions is inserted into a sorted `LinkedList` via
`insertSorted()`, which does a linear scan `O(k)` where `k` is the current list
length.

Total: `O(1 + 2 + … + n) = O(n²)`.

**Fix hint**: collect into a plain array and call `usort()` — Timsort runs in
`O(n log n)`.

---

### Bottleneck 5 — Category Report `O(cat × n)`

**Location**: `BatchProcessor::buildCategoryReport()`

The unique categories are discovered by a `LinkedList` scan (`O(cat × n)`).
Then for each category, `array_filter` iterates over **all** `n` transactions.

Total: `O(cat × n)`.  Although `cat = 8` is a constant here, the design pattern
itself is quadratic when categories grow with `n`.

**Fix hint**: a single pass with `$report[$tx->category]++` runs in `O(n)`.

---

## Project Structure

```
php/
├── composer.json
├── phpunit.xml
├── src/
│   ├── index.php              # CLI entry point
│   ├── BatchProcessor.php     # Main processor — all 5 bottlenecks live here
│   ├── Transaction.php        # Immutable value object
│   ├── ProcessingResult.php   # Result aggregate
│   ├── DataGenerator.php      # Synthetic CSV generator
│   └── DataStructures/
│       ├── LinkedList.php     # Custom O(n) linked list
│       ├── Node.php           # Internal linked-list node
│       └── AdjacencyMatrix.php # O(V²) adjacency matrix graph
├── tests/
│   ├── CorrectnessTest.php    # Business logic assertions (100 records)
│   └── PerformanceTest.php    # Timing benchmarks (500 / 2 000 / 10 000)
├── data/                      # Generated CSV files go here
└── README.md
```

---

## Scoring Rubric

| Criterion                        | Points |
|----------------------------------|--------|
| All `CorrectnessTest` pass       | 40     |
| 10 000 tx in < 15 s              | 20     |
| 10 000 tx in < 5 s               | 20     |
| 1 000 000 tx in < 60 s           | 20     |

---

## Tips

1. Run the correctness tests after **every** change — performance improvements
   must not break correctness.
2. Profile with `xdebug` or `Blackfire` to confirm which bottleneck is the
   biggest contributor on your machine.
3. Replace one bottleneck at a time and re-run the performance test to measure
   the incremental gain.
4. The custom `LinkedList` and `AdjacencyMatrix` classes must be **kept** but
   can be **supplemented** with better data structures in `BatchProcessor`.

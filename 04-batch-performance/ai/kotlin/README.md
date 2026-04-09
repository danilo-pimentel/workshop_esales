# Batch Transaction Processor – Performance Challenge

A financial transaction batch processor written in Kotlin that intentionally
contains **five O(n²) algorithmic bottlenecks**. Your challenge is to profile,
identify, and fix each bottleneck so the processor meets the performance target.

---

## The Challenge

| Dataset    | Current (broken) | Target (optimised) |
|------------|------------------|--------------------|
| 1 000 tx   | ~500 ms          | < 50 ms            |
| 10 000 tx  | **~20 seconds**  | **< 500 ms**       |
| 100 000 tx | ~30 minutes      | < 5 seconds        |
| 1 000 000 tx | impossible     | < 60 seconds       |

Fix all five bottlenecks and make `10 000 transactions process in under 500 ms`.

---

## Prerequisites

| Tool | Version |
|------|---------|
| JDK  | 21+     |
| Gradle | 8.x (wrapper included) |

Verify with:

```bash
java -version   # should report 21+

# Linux / macOS:
./gradlew --version

# Windows (PowerShell):
.\gradlew --version
```

---

## Quick Start

> **Nota Windows:** Em todos os comandos abaixo, substitua `./gradlew` por `.\gradlew`.

### 1. Generate a dataset

```bash
# Linux / macOS:
./gradlew run --args="--generate data/transactions_10k.csv 10000"

# Windows (PowerShell):
.\gradlew run --args="--generate data/transactions_10k.csv 10000"
```

Other sizes:

```bash
# Linux / macOS:
./gradlew run --args="--generate data/transactions_1k.csv 1000"
./gradlew run --args="--generate data/transactions_100k.csv 100000"

# Windows (PowerShell):
.\gradlew run --args="--generate data/transactions_1k.csv 1000"
.\gradlew run --args="--generate data/transactions_100k.csv 100000"
```

### 2. Run the processor

```bash
# Linux / macOS:
./gradlew run --args="data/transactions_10k.csv"

# Windows (PowerShell):
.\gradlew run --args="data/transactions_10k.csv"
```

### 3. Generate + run in one step

```bash
# Linux / macOS:
./gradlew run --args="--generate-and-run data/transactions_10k.csv 10000"

# Windows (PowerShell):
.\gradlew run --args="--generate-and-run data/transactions_10k.csv 10000"
```

### 4. Run tests

```bash
# Linux / macOS:
./gradlew test

# Windows (PowerShell):
.\gradlew test
```

Run only correctness tests (fast):

```bash
# Linux / macOS:
./gradlew test --tests "com.treinamento.batch.CorrectnessTest"

# Windows (PowerShell):
.\gradlew test --tests "com.treinamento.batch.CorrectnessTest"
```

Run only performance benchmarks:

```bash
# Linux / macOS:
./gradlew test --tests "com.treinamento.batch.PerformanceTest"

# Windows (PowerShell):
.\gradlew test --tests "com.treinamento.batch.PerformanceTest"
```

---

## Project Structure

```
kotlin/
├── build.gradle.kts
├── settings.gradle.kts
├── data/                              # Generated CSV files (git-ignored)
└── src/
    ├── main/kotlin/com/treinamento/batch/
    │   ├── Main.kt                    # CLI entry point
    │   ├── BatchProcessor.kt          # ← 5 bottlenecks live here
    │   ├── Transaction.kt             # Data class + CSV parsing
    │   ├── ProcessingResult.kt        # Result aggregate
    │   ├── DataGenerator.kt           # Synthetic CSV generator
    │   └── datastructures/
    │       ├── LinkedList.kt          # Custom singly-linked list (all O(n))
    │       └── AdjacencyMatrix.kt     # Dense boolean adjacency matrix
    └── test/kotlin/com/treinamento/batch/
        ├── CorrectnessTest.kt         # ~25 unit tests, all must PASS
        └── PerformanceTest.kt         # Timing benchmarks, always PASS
```

---

## The Five Bottlenecks

All bottlenecks are in `BatchProcessor.kt`. The code is **correct**; the
problem is **algorithmic complexity**.

### Bottleneck 1 — Duplicate Detection `O(n²)`

**Location:** `detectDuplicates()`

For each of the *n* incoming transactions, a custom `LinkedList<Long>` is
scanned from head to tail to check whether the transaction id was already seen.
Total comparisons: 1 + 2 + 3 + … + n = **O(n²)**.

**Fix hint:** Replace the `LinkedList<Long>` with a `HashSet<Long>`. Lookup
becomes O(1) amortised → total **O(n)**.

---

### Bottleneck 2 — Account Balance Calculation `O(n²)`

**Location:** `computeAccountBalances()`

Account entries are stored in a `LinkedList<Pair<String, Double>>` kept sorted
by account id. For every transaction:
- `find()` → O(n) scan
- `removeIf()` → O(n) scan
- `insertSorted()` → O(n) scan

…repeated for both source and destination account → **O(n)** per transaction →
**O(n²)** total.

**Fix hint:** Use a `HashMap<String, Double>`. All three operations become O(1).

---

### Bottleneck 3 — Fraud Ring Detection `O(V² + V×E)`

**Location:** `detectFraudRings()`

1. An `AdjacencyMatrix` (dense 2-D `BooleanArray`) stores V×V booleans even
   for a sparse transaction graph.
2. `getNeighbors(v)` scans the entire row → O(V) per call.
3. DFS is launched from **every vertex** independently → O(V) × O(V+E) = **O(V²+V×E)**.
4. Account index lookups use a `LinkedList` find → O(V) per lookup.

**Fix hint:** Use an adjacency list (`HashMap<String, MutableList<String>>`),
run **Tarjan's SCC algorithm** (single DFS pass → **O(V+E)**).

---

### Bottleneck 4 — Priority Sort `O(n²)`

**Location:** `sortByPriority()`

Each transaction is inserted into a sorted `LinkedList` via `insertSorted()`,
which scans from the head to find the insertion point → O(n) per insert.
Inserting n elements: 1 + 2 + … + n = **O(n²)**.

**Fix hint:** Collect into a `MutableList` and call `sortedWith()` →
**O(n log n)**.

---

### Bottleneck 5 — Category Report `O(cat × n)`

**Location:** `buildCategoryReport()`

Step 1 collects distinct categories with a `LinkedList` scan (O(n) find per
transaction → O(n²) worst case for unique categories). Step 2 scans the entire
transaction list once per category → O(cat × n). With 8 fixed categories this
is O(8n) ≈ O(n), but the pattern is the canonical anti-pattern that blows up
when categories scale with n.

**Fix hint:** Single pass with `mutableMapOf<String, Int>()` using
`getOrDefault` and `put` → **O(n)**.

---

## Profiling Tips

### IntelliJ IDEA Profiler

1. Open the project in IntelliJ IDEA.
2. Run → Edit Configurations → add a Gradle run config.
3. Click **Run with Profiler** (CPU Sampling).
4. Look for methods spending the most cumulative time.

### Command-line with async-profiler

```bash
./gradlew run --args="data/transactions_10k.csv" \
    -Dorg.gradle.jvmargs="-agentpath:/path/to/libasyncProfiler.so=start,event=cpu,file=/tmp/flamegraph.html"
```

### JVM flags for heap inspection

```bash
./gradlew run --args="data/transactions_10k.csv" \
    -Dorg.gradle.jvmargs="-Xss8m -verbose:gc"
```

---

## Scoring

| Criterion | Points |
|-----------|--------|
| All `CorrectnessTest` tests still PASS | 40 |
| 10k dataset processes in < 15 s       | 10 |
| 10k dataset processes in < 5 s        | 10 |
| 10k dataset processes in < 1 s        | 20 |
| 10k dataset processes in < 500 ms     | 20 |
| **Total**                             | **100** |

---

## Rules

- You **may** change `BatchProcessor.kt` freely.
- You **may** add new data structures / utility classes.
- You **must not** change `Transaction.kt`, `ProcessingResult.kt`, or any test
  files.
- All `CorrectnessTest` tests must continue to pass.
- Using `kotlin.collections.HashMap` / `HashSet` / `TreeMap` in `BatchProcessor`
  is allowed — that is the intended fix direction.
- Replacing the custom `LinkedList` and `AdjacencyMatrix` in `BatchProcessor` is
  allowed; the custom data structures in `datastructures/` must remain intact
  (they are tested independently).

---

## Expected Output (slow baseline)

```
Reading transactions from: data/transactions_10k.csv
Loaded 10000 transactions in 312 ms

Starting batch processing…
(This may take a while – the processor contains intentional O(n²) bottlenecks)

=== Batch Processing Result ===
Total input transactions : 10000
Duplicates removed       : 199
Unique transactions      : 9801
Accounts tracked         : 1000
Fraud rings detected     : 47
Category breakdown:
  RETAIL       : 1236
  WIRE         : 1221
  ...
Total processing time    : 22418 ms
--- Bottleneck breakdown ---
  1_duplicate_detection          : 4832 ms
  2_account_balances             : 8941 ms
  3_fraud_ring_detection         : 6103 ms
  4_priority_sort                : 2187 ms
  5_category_report              : 355 ms

*** PERFORMANCE WARNING ***
Processing took 22418 ms.
```

Good luck!

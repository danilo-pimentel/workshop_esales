# Batch Performance Exercise — Java 21

## Context

You have inherited a **financial transaction batch processor** that is
**correct** but **catastrophically slow** at scale.

The system reads a CSV file of bank transactions and:

1. Detects and rejects **duplicate transactions** (same source, destination,
   amount within a 5-second window).
2. Tracks **account balances** and rejects transactions where the source
   account has insufficient funds.
3. Performs **fraud-ring detection** by building a transaction graph and
   running DFS to find cycles.
4. **Orders accepted transactions** by priority (5 = highest, 1 = lowest).
5. Produces a **category summary report** (count, total and average amount per
   category).

All of this works correctly. The problem is **performance**.

---

## The Challenge

| Dataset size | Current time  | Target time  |
|:------------:|:-------------:|:------------:|
| 1 000        | ~0.2 s        | < 0.1 s      |
| 10 000       | **~20–60 s**  | **< 2 s**    |
| 100 000      | ~30 minutes   | < 10 s       |
| 1 000 000    | not feasible  | **< 15 s**   |

Your task: **optimise the processor so that 1 000 000 records completes in
under 15 seconds**, without changing the business logic.

---

## The 5 Bottlenecks

Every bottleneck lives inside `BatchProcessor.java`.

### Bottleneck 1 — Duplicate Detection: O(n²)

```
LinkedList<Transaction> recentTransactions = ...
recentTransactions.find(...)   // O(n) per transaction
```

**Fix hint:** Replace the LinkedList with a structure that gives O(1) lookup
on `"src|dst|amount"` and evicts entries older than 5 seconds.

### Bottleneck 2 — Account Balance Tracking: O(n²)

```
LinkedList<AccountBalance> balances = ...
balances.find(ab -> ab.accountId.equals(...))   // O(n) per transaction
```

**Fix hint:** Use a `HashMap<String, Double>` — O(1) lookup.

### Bottleneck 3 — Fraud Ring Detection: O(V² + V·E)

```
AdjacencyMatrix graph = new AdjacencyMatrix(MAX_ACCOUNTS);
// getNeighbors is O(V) — runs inside DFS from every vertex
```

**Fix hint:** Use an adjacency list (`HashMap<Integer, List<Integer>>`).
Run a single-pass cycle detection (Kahn's / Tarjan's) instead of DFS from
every vertex.

### Bottleneck 4 — Priority Queue via Insertion Sort: O(n²)

```
priorityQueue.insertSorted(tx, PRIORITY_COMPARATOR)  // O(n) per insert
```

**Fix hint:** Use `java.util.PriorityQueue` — O(log n) per insert.

### Bottleneck 5 — Category Report: O(categories × n)

```
for (String category : cats) {          // O(categories)
    for (Transaction tx : transactions) { // O(n) each
        if (tx.category().equals(category)) { ... }
    }
}
```

**Fix hint:** One pass with `HashMap<String, DoubleSummaryStatistics>` —
O(n) total.

---

## Rules

- **Do not change the business logic.** The `CorrectnessTest` suite must
  continue to pass after your optimisation.
- You MAY replace `datastructures.LinkedList` and `datastructures.AdjacencyMatrix`
  with standard Java collections — the point of the exercise is recognising
  and fixing the algorithmic complexity, not keeping the custom data structures.
- You MAY NOT skip validation steps (duplicate check, balance check, fraud
  detection).
- You MAY NOT hard-code results or pre-sort the input.

---

## Prerequisites

| Tool    | Version |
|---------|---------|
| JDK     | 21+     |
| Maven   | 3.9+    |

---

## Commands

### Generate a dataset

```bash
# Linux / macOS:
mvn exec:java \
    -Dexec.mainClass=com.treinamento.batch.DataGenerator \
    -Dexec.args="10000 data/transactions_10k.csv 42"

# Windows (PowerShell) — use aspas nos parametros -D:
mvn exec:java "-Dexec.mainClass=com.treinamento.batch.DataGenerator" "-Dexec.args=10000 data/transactions_10k.csv 42"
```

```bash
# 1 000 000 rows
# Linux / macOS:
mvn exec:java \
    -Dexec.mainClass=com.treinamento.batch.DataGenerator \
    -Dexec.args="1000000 data/transactions_1m.csv 42"

# Windows (PowerShell):
mvn exec:java "-Dexec.mainClass=com.treinamento.batch.DataGenerator" "-Dexec.args=1000000 data/transactions_1m.csv 42"
```

### Run the processor

```bash
# Linux / macOS:
mvn exec:java -Dexec.args="data/transactions_10k.csv"

# Windows (PowerShell):
mvn exec:java "-Dexec.args=data/transactions_10k.csv"
```

### Run tests

```bash
# All tests
mvn test

# Correctness only (fast)
# Linux / macOS:
mvn test -Dtest=CorrectnessTest
# Windows (PowerShell):
mvn test "-Dtest=CorrectnessTest"

# Performance only
# Linux / macOS:
mvn test -Dtest=PerformanceTest
# Windows (PowerShell):
mvn test "-Dtest=PerformanceTest"
```

### Build a fat JAR

```bash
mvn package
java -jar target/batch-performance-1.0.0-SNAPSHOT.jar data/transactions_10k.csv
```

---

## File Structure

```
java/
├── pom.xml
├── src/main/java/com/treinamento/batch/
│   ├── Main.java               CLI entry point
│   ├── BatchProcessor.java     ← all 5 bottlenecks live here
│   ├── Transaction.java        immutable record (CSV model)
│   ├── ProcessingResult.java   result record with category reports
│   ├── DataGenerator.java      synthetic dataset generator
│   └── datastructures/
│       ├── LinkedList.java     O(n) custom linked list
│       └── AdjacencyMatrix.java  O(V²) adjacency matrix
└── src/test/java/com/treinamento/batch/
    ├── CorrectnessTest.java    19 correctness assertions (must stay GREEN)
    └── PerformanceTest.java    timing test for 10k records
```

---

## How to Measure Your Progress

After each optimisation run the performance test and compare wall-clock times:

```bash
mvn test -Dtest=PerformanceTest -pl . 2>&1 | grep -E "Wall time|Reported"
```

A successful optimisation of all 5 bottlenecks should yield:

```
Wall time         : 87 ms        (was ~30 000 ms)
```

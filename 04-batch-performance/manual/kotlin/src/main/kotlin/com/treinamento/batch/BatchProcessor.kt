package com.treinamento.batch

import com.treinamento.batch.datastructures.AdjacencyMatrix
import com.treinamento.batch.datastructures.LinkedList

/**
 * Financial transaction batch processor.
 */
class BatchProcessor {

    companion object {
        // Initial balance granted to every account that appears in the data set
        private const val INITIAL_BALANCE = 10_000.0

        // Two transactions are considered duplicates if they share the same source,
        // destination and amount, and their timestamps differ by less than 5 seconds.
        private const val DUPLICATE_WINDOW_MS = 5_000L
    }

    /**
     * Processes the given [transactions] through all five pipeline stages
     * and returns a [ProcessingResult].
     */
    fun process(transactions: List<Transaction>): ProcessingResult {
        val total = transactions.size

        // Stage 1: Duplicate detection
        val s1 = System.nanoTime()
        val (unique, duplicateCount) = detectDuplicates(transactions)
        val t1 = (System.nanoTime() - s1) / 1_000_000_000.0
        print("\r  [1/5] Detectando duplicatas... 100% ($total/$total) ${"%.3f".format(java.util.Locale.US, t1)}s\n")
        System.out.flush()

        // Stage 2: Account balance validation
        val s2 = System.nanoTime()
        val (accepted, rejectedCount) = validateBalances(unique)
        val t2 = (System.nanoTime() - s2) / 1_000_000_000.0
        print("\r  [2/5] Validando saldos... 100% (${unique.size}/${unique.size}) ${"%.3f".format(java.util.Locale.US, t2)}s\n")
        System.out.flush()

        // Stage 3: Fraud ring detection
        val s3 = System.nanoTime()
        val fraudRings = detectFraudRings(accepted)
        val t3 = (System.nanoTime() - s3) / 1_000_000_000.0
        print("\r  [3/5] Detectando aneis de fraude... 100% ${"%.3f".format(java.util.Locale.US, t3)}s\n")
        System.out.flush()

        // Stage 4: Priority sorting
        val s4 = System.nanoTime()
        val sorted = sortByPriority(accepted)
        val t4 = (System.nanoTime() - s4) / 1_000_000_000.0
        print("\r  [4/5] Ordenando por prioridade... 100% (${accepted.size}/${accepted.size}) ${"%.3f".format(java.util.Locale.US, t4)}s\n")
        System.out.flush()

        // Stage 5: Category report
        val s5 = System.nanoTime()
        val report = generateReport(sorted)
        val t5 = (System.nanoTime() - s5) / 1_000_000_000.0
        print("\r  [5/5] Gerando relatorio por categoria... 100% ${"%.3f".format(java.util.Locale.US, t5)}s\n")
        System.out.flush()

        return ProcessingResult(
            processed = accepted.size,
            duplicates = duplicateCount,
            rejected = rejectedCount,
            fraudRings = fraudRings,
            report = report
        )
    }

    // -----------------------------------------------------------------------
    // Stage 1 -- Duplicate Detection
    // -----------------------------------------------------------------------
    private fun detectDuplicates(
        transactions: List<Transaction>
    ): Pair<List<Transaction>, Int> {
        val seen = LinkedList<Transaction>()
        val unique = mutableListOf<Transaction>()
        var duplicateCount = 0

        val total = transactions.size
        val interval = maxOf(total / 20, 1)

        for (i in transactions.indices) {
            if (i % interval == 0) {
                print("\r  [1/5] Detectando duplicatas... ${i * 100 / total}% ($i/$total)")
                System.out.flush()
            }

            val tx = transactions[i]

            // O(n) scan of the LinkedList for each transaction
            val isDup = seen.find { prev ->
                prev.sourceAccount == tx.sourceAccount &&
                prev.destAccount == tx.destAccount &&
                prev.amount == tx.amount &&
                Math.abs(prev.timestamp - tx.timestamp) < DUPLICATE_WINDOW_MS
            }

            if (isDup != null) {
                duplicateCount++
            } else {
                unique.add(tx)
                seen.append(tx)
            }
        }

        return Pair(unique, duplicateCount)
    }

    // -----------------------------------------------------------------------
    // Stage 2 -- Account Balance Validation
    // -----------------------------------------------------------------------

    private class AccountBalance(val accountId: String, var balance: Double)

    private fun validateBalances(
        transactions: List<Transaction>
    ): Pair<List<Transaction>, Int> {
        val balances = LinkedList<AccountBalance>()
        val accepted = mutableListOf<Transaction>()
        var rejectedCount = 0

        val total = transactions.size
        val interval = maxOf(total / 20, 1)

        for (i in transactions.indices) {
            if (i % interval == 0) {
                print("\r  [2/5] Validando saldos... ${i * 100 / total}% ($i/$total)")
                System.out.flush()
            }

            val tx = transactions[i]

            // Ensure both accounts exist (O(n) find + O(n) insertSorted each)
            ensureAccount(balances, tx.sourceAccount)
            ensureAccount(balances, tx.destAccount)

            // Check source has sufficient funds -- O(n) scan
            val sourceEntry = balances.find { it.accountId == tx.sourceAccount }
            if (sourceEntry == null || sourceEntry.balance < tx.amount) {
                rejectedCount++
                continue
            }

            // Debit source
            sourceEntry.balance -= tx.amount

            // Credit destination -- O(n) scan
            val destEntry = balances.find { it.accountId == tx.destAccount }
            destEntry!!.balance += tx.amount

            accepted.add(tx)
        }

        return Pair(accepted, rejectedCount)
    }

    /** Lazily initialise an account with INITIAL_BALANCE (sorted insert, O(n)). */
    private fun ensureAccount(balances: LinkedList<AccountBalance>, accountId: String) {
        val existing = balances.find { it.accountId == accountId }
        if (existing == null) {
            balances.insertSorted(
                AccountBalance(accountId, INITIAL_BALANCE),
                Comparator { a, b -> a.accountId.compareTo(b.accountId) }
            )
        }
    }

    // -----------------------------------------------------------------------
    // Stage 3 -- Fraud Ring Detection
    // -----------------------------------------------------------------------
    private fun detectFraudRings(transactions: List<Transaction>): Int {
        if (transactions.isEmpty()) return 0

        // Collect unique accounts
        val accountSet = mutableSetOf<String>()
        for (tx in transactions) {
            accountSet.add(tx.sourceAccount)
            accountSet.add(tx.destAccount)
        }
        val accounts = accountSet.toTypedArray()

        // Build adjacency matrix -- O(V^2) allocation
        val graph = AdjacencyMatrix(accounts)
        for (tx in transactions) {
            graph.addEdge(tx.sourceAccount, tx.destAccount)
        }

        var ringCount = 0
        val globalVisited = mutableSetOf<String>()
        val totalV = accounts.size
        val intervalV = maxOf(totalV / 20, 1)

        // DFS from every node -- O(V * (V+E)) total
        for (vi in 0 until totalV) {
            if (vi % intervalV == 0) {
                print("\r  [3/5] Detectando aneis de fraude... ${vi * 100 / totalV}% ($vi/$totalV)")
                System.out.flush()
            }
            val startNode = accounts[vi]
            if (globalVisited.contains(startNode)) continue

            val visited = mutableSetOf<String>()
            val path = mutableListOf<String>()
            val ringsFoundInThisDfs = mutableSetOf<String>()

            fun dfs(node: String) {
                visited.add(node)
                path.add(node)

                // O(V) scan of the matrix row for this node
                val neighbours = graph.getNeighbors(node)
                for (neighbour in neighbours) {
                    if (!visited.contains(neighbour)) {
                        dfs(neighbour)
                    } else {
                        // Found a back-edge -> cycle detected
                        val cycleStart = path.indexOf(neighbour)
                        if (cycleStart != -1) {
                            val cycle = path.subList(cycleStart, path.size)
                            val cycleKey = cycle.sorted().joinToString(",")
                            if (!ringsFoundInThisDfs.contains(cycleKey)) {
                                ringsFoundInThisDfs.add(cycleKey)
                                ringCount++
                            }
                        }
                    }
                }

                path.removeAt(path.size - 1)
                globalVisited.add(node)
            }

            dfs(startNode)
        }

        return ringCount
    }

    // -----------------------------------------------------------------------
    // Stage 4 -- Priority Sorting
    // -----------------------------------------------------------------------
    private fun sortByPriority(transactions: List<Transaction>): List<Transaction> {
        val sorted = LinkedList<Transaction>()
        val total = transactions.size
        val interval = maxOf(total / 20, 1)

        for (i in transactions.indices) {
            if (i % interval == 0) {
                print("\r  [4/5] Ordenando por prioridade... ${i * 100 / total}% ($i/$total)")
                System.out.flush()
            }
            // Higher priority number = processed first (descending sort)
            sorted.insertSorted(transactions[i], Comparator { a, b -> b.priority - a.priority })
        }

        return sorted.toList()
    }

    // -----------------------------------------------------------------------
    // Stage 5 -- Category Report
    // -----------------------------------------------------------------------
    private fun generateReport(transactions: List<Transaction>): List<CategoryStats> {
        if (transactions.isEmpty()) return emptyList()

        // Collect unique categories with a linear scan, then sort
        val uniqueCategories = transactions.map { it.category }.toSet().sorted()

        val stats = mutableListOf<CategoryStats>()
        val totalCat = uniqueCategories.size

        // For each category, do a full second pass -- O(n) per category
        for (ci in uniqueCategories.indices) {
            print("\r  [5/5] Gerando relatorio por categoria... ${ci * 100 / totalCat}% ($ci/$totalCat)")
            System.out.flush()

            val cat = uniqueCategories[ci]
            val filtered = transactions.filter { it.category == cat }

            var total = 0.0
            for (t in filtered) {
                total += t.amount
            }

            // Round to 2 decimal places like Bun: parseFloat(total.toFixed(2))
            val roundedTotal = "%.2f".format(java.util.Locale.US, total).toDouble()
            val roundedAvg = "%.2f".format(java.util.Locale.US, total / filtered.size).toDouble()

            stats.add(CategoryStats(
                category = cat,
                count = filtered.size,
                total = roundedTotal,
                average = roundedAvg
            ))
        }

        return stats
    }
}

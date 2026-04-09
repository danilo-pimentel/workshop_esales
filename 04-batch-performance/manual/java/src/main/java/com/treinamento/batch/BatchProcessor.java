package com.treinamento.batch;

import com.treinamento.batch.datastructures.AdjacencyMatrix;
import com.treinamento.batch.datastructures.LinkedList;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Main batch processor for financial transactions.
 */
public class BatchProcessor {

    // -------------------------------------------------------------------------
    // Inner helper types
    // -------------------------------------------------------------------------

    /** Mutable account balance -- stored in the sorted LinkedList. */
    private static class AccountBalance {
        final String accountId;
        double balance;

        AccountBalance(String accountId, double initialBalance) {
            this.accountId = accountId;
            this.balance = initialBalance;
        }
    }

    // -------------------------------------------------------------------------
    // Configuration constants
    // -------------------------------------------------------------------------

    private static final long DUPLICATE_WINDOW_MS = 5_000L;
    private static final double INITIAL_BALANCE = 10_000.0;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Reads all transaction lines from {@code csvPath} and processes them.
     */
    public ProcessingResult process(Path csvPath) throws IOException {
        List<Transaction> transactions = loadTransactions(csvPath);
        return processTransactions(transactions);
    }

    /**
     * Processes a pre-loaded list of transactions.
     * Exposed for testing so tests can inject data without touching the file system.
     */
    public ProcessingResult processTransactions(List<Transaction> transactions) {
        int total = transactions.size();

        // Stage 1: Duplicate detection
        long s1 = System.currentTimeMillis();
        DedupResult dedupResult = detectDuplicates(transactions);
        System.out.printf(java.util.Locale.US, "\r  [1/5] Detectando duplicatas... 100%% (%d/%d) %.3fs%n",
            total, total, (System.currentTimeMillis() - s1) / 1000.0);

        // Stage 2: Balance validation
        long s2 = System.currentTimeMillis();
        BalanceResult balanceResult = validateBalances(dedupResult.unique);
        System.out.printf(java.util.Locale.US, "\r  [2/5] Validando saldos... 100%% (%d/%d) %.3fs%n",
            dedupResult.unique.size(), dedupResult.unique.size(),
            (System.currentTimeMillis() - s2) / 1000.0);

        // Stage 3: Fraud ring detection
        long s3 = System.currentTimeMillis();
        int fraudRings = detectFraudRings(balanceResult.accepted);
        System.out.printf(java.util.Locale.US, "\r  [3/5] Detectando aneis de fraude... 100%% %.3fs%n",
            (System.currentTimeMillis() - s3) / 1000.0);

        // Stage 4: Priority sorting
        long s4 = System.currentTimeMillis();
        List<Transaction> sorted = sortByPriority(balanceResult.accepted);
        System.out.printf(java.util.Locale.US, "\r  [4/5] Ordenando por prioridade... 100%% (%d/%d) %.3fs%n",
            balanceResult.accepted.size(), balanceResult.accepted.size(),
            (System.currentTimeMillis() - s4) / 1000.0);

        // Stage 5: Category report
        long s5 = System.currentTimeMillis();
        List<ProcessingResult.CategoryReport> report = generateReport(sorted);
        System.out.printf(java.util.Locale.US, "\r  [5/5] Gerando relatorio por categoria... 100%% %.3fs%n",
            (System.currentTimeMillis() - s5) / 1000.0);

        return new ProcessingResult(
            balanceResult.accepted.size(),
            dedupResult.duplicateCount,
            balanceResult.rejectedCount,
            fraudRings,
            report
        );
    }

    // -------------------------------------------------------------------------
    // Stage 1 -- Duplicate Detection
    // -------------------------------------------------------------------------

    private DedupResult detectDuplicates(List<Transaction> transactions) {
        LinkedList<Transaction> seen = new LinkedList<>();
        List<Transaction> unique = new ArrayList<>();
        int duplicateCount = 0;
        int total = transactions.size();
        int interval = Math.max(total / 20, 1);

        for (int i = 0; i < total; i++) {
            if (i % interval == 0) {
                System.out.printf("\r  [1/5] Detectando duplicatas... %d%% (%d/%d)",
                    i * 100 / total, i, total);
                System.out.flush();
            }
            Transaction tx = transactions.get(i);
            final Transaction ftx = tx;

            // O(n) scan of LinkedList for each transaction
            Transaction isDup = seen.find(prev ->
                prev.sourceAccount().equals(ftx.sourceAccount()) &&
                prev.destAccount().equals(ftx.destAccount()) &&
                Double.compare(prev.amount(), ftx.amount()) == 0 &&
                Math.abs(prev.timestamp() - ftx.timestamp()) < DUPLICATE_WINDOW_MS
            );

            if (isDup != null) {
                duplicateCount++;
            } else {
                unique.add(tx);
                seen.append(tx);
            }
        }
        return new DedupResult(unique, duplicateCount);
    }

    // -------------------------------------------------------------------------
    // Stage 2 -- Balance Validation
    // -------------------------------------------------------------------------

    private BalanceResult validateBalances(List<Transaction> transactions) {
        LinkedList<AccountBalance> balances = new LinkedList<>();
        List<Transaction> accepted = new ArrayList<>();
        int rejectedCount = 0;
        int total = transactions.size();
        int interval = Math.max(total / 20, 1);

        Comparator<AccountBalance> comp = Comparator.comparing(ab -> ab.accountId);

        for (int i = 0; i < total; i++) {
            if (i % interval == 0) {
                System.out.printf("\r  [2/5] Validando saldos... %d%% (%d/%d)",
                    i * 100 / total, i, total);
                System.out.flush();
            }
            Transaction tx = transactions.get(i);
            final Transaction ftx = tx;

            // Ensure both accounts exist -- O(n) find + O(n) insertSorted each
            ensureAccount(balances, tx.sourceAccount(), comp);
            ensureAccount(balances, tx.destAccount(), comp);

            // Check source has sufficient funds -- O(n) scan
            AccountBalance srcBalance = balances.find(b -> b.accountId.equals(ftx.sourceAccount()));
            if (srcBalance == null || srcBalance.balance < tx.amount()) {
                rejectedCount++;
                continue;
            }

            // Debit source, credit dest
            srcBalance.balance -= tx.amount();
            AccountBalance dstBalance = balances.find(b -> b.accountId.equals(ftx.destAccount()));
            dstBalance.balance += tx.amount();

            accepted.add(tx);
        }
        return new BalanceResult(accepted, rejectedCount);
    }

    private void ensureAccount(LinkedList<AccountBalance> balances, String accountId,
                               Comparator<AccountBalance> comp) {
        AccountBalance existing = balances.find(b -> b.accountId.equals(accountId));
        if (existing == null) {
            balances.insertSorted(new AccountBalance(accountId, INITIAL_BALANCE), comp);
        }
    }

    // -------------------------------------------------------------------------
    // Stage 3 -- Fraud Ring Detection
    // -------------------------------------------------------------------------

    private int detectFraudRings(List<Transaction> transactions) {
        if (transactions.isEmpty()) return 0;

        // Collect unique accounts
        Set<String> accountSet = new java.util.LinkedHashSet<>();
        for (Transaction tx : transactions) {
            accountSet.add(tx.sourceAccount());
            accountSet.add(tx.destAccount());
        }
        String[] accounts = accountSet.toArray(new String[0]);

        // Build adjacency matrix -- O(V^2) allocation
        AdjacencyMatrix graph = new AdjacencyMatrix(accounts);
        for (Transaction tx : transactions) {
            graph.addEdge(tx.sourceAccount(), tx.destAccount());
        }

        int ringCount = 0;
        Set<String> globalVisited = new HashSet<>();
        int totalV = accounts.length;
        int intervalV = Math.max(totalV / 20, 1);

        // DFS from every node
        for (int vi = 0; vi < totalV; vi++) {
            if (vi % intervalV == 0) {
                System.out.printf("\r  [3/5] Detectando aneis de fraude... %d%% (%d/%d)",
                    vi * 100 / totalV, vi, totalV);
                System.out.flush();
            }
            String startNode = accounts[vi];
            if (globalVisited.contains(startNode)) continue;

            Set<String> visited = new HashSet<>();
            List<String> path = new ArrayList<>();
            Set<String> ringsFoundInThisDfs = new HashSet<>();

            ringCount += dfs(graph, startNode, visited, path, ringsFoundInThisDfs, globalVisited);
        }

        return ringCount;
    }

    private int dfs(AdjacencyMatrix graph, String node,
                    Set<String> visited, List<String> path,
                    Set<String> ringsFoundInThisDfs, Set<String> globalVisited) {
        visited.add(node);
        path.add(node);
        int rings = 0;

        String[] neighbours = graph.getNeighbors(node);
        for (String neighbour : neighbours) {
            if (!visited.contains(neighbour)) {
                rings += dfs(graph, neighbour, visited, path, ringsFoundInThisDfs, globalVisited);
            } else {
                int cycleStart = path.indexOf(neighbour);
                if (cycleStart != -1) {
                    List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                    Collections.sort(cycle);
                    String cycleKey = String.join(",", cycle);
                    if (!ringsFoundInThisDfs.contains(cycleKey)) {
                        ringsFoundInThisDfs.add(cycleKey);
                        rings++;
                    }
                }
            }
        }

        path.remove(path.size() - 1);
        globalVisited.add(node);
        return rings;
    }

    // -------------------------------------------------------------------------
    // Stage 4 -- Priority Sorting
    // -------------------------------------------------------------------------

    private List<Transaction> sortByPriority(List<Transaction> transactions) {
        LinkedList<Transaction> sorted = new LinkedList<>();
        int total = transactions.size();
        int interval = Math.max(total / 20, 1);

        // Higher priority = processed first (descending sort)
        Comparator<Transaction> comp = (a, b) -> b.priority() - a.priority();

        for (int i = 0; i < total; i++) {
            if (i % interval == 0) {
                System.out.printf("\r  [4/5] Ordenando por prioridade... %d%% (%d/%d)",
                    i * 100 / total, i, total);
                System.out.flush();
            }
            sorted.insertSorted(transactions.get(i), comp);
        }

        // Convert LinkedList to ArrayList
        Object[] arr = sorted.toArray();
        List<Transaction> result = new ArrayList<>(arr.length);
        for (Object o : arr) result.add((Transaction) o);
        return result;
    }

    // -------------------------------------------------------------------------
    // Stage 5 -- Category Report
    // -------------------------------------------------------------------------

    private List<ProcessingResult.CategoryReport> generateReport(List<Transaction> transactions) {
        if (transactions.isEmpty()) return List.of();

        // Collect unique categories
        Set<String> catSet = new java.util.LinkedHashSet<>();
        for (Transaction t : transactions) catSet.add(t.category());
        List<String> uniqueCategories = new ArrayList<>(catSet);
        Collections.sort(uniqueCategories);

        List<ProcessingResult.CategoryReport> stats = new ArrayList<>();
        int totalCat = uniqueCategories.size();

        for (int ci = 0; ci < totalCat; ci++) {
            System.out.printf("\r  [5/5] Gerando relatorio por categoria... %d%% (%d/%d)",
                ci * 100 / totalCat, ci, totalCat);
            System.out.flush();
            String cat = uniqueCategories.get(ci);

            int count = 0;
            double total = 0;
            for (Transaction t : transactions) {
                if (t.category().equals(cat)) {
                    count++;
                    total += t.amount();
                }
            }

            double avg = total / count;
            // Match Bun's parseFloat(total.toFixed(2)) behavior
            total = Math.round(total * 100.0) / 100.0;
            avg = Math.round(avg * 100.0) / 100.0;

            stats.add(new ProcessingResult.CategoryReport(cat, count, total, avg));
        }

        return stats;
    }

    // -------------------------------------------------------------------------
    // Helper types
    // -------------------------------------------------------------------------

    private record DedupResult(List<Transaction> unique, int duplicateCount) {}
    private record BalanceResult(List<Transaction> accepted, int rejectedCount) {}

    // -------------------------------------------------------------------------
    // CSV loading
    // -------------------------------------------------------------------------

    private List<Transaction> loadTransactions(Path path) throws IOException {
        List<Transaction> list = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("id,")) continue;
                try {
                    list.add(Transaction.fromCsv(trimmed));
                } catch (Exception e) {
                    // skip malformed rows
                }
            }
        }
        return list;
    }
}

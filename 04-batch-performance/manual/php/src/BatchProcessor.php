<?php

declare(strict_types=1);

namespace BatchProcessor;

use BatchProcessor\DataStructures\AdjacencyMatrix;
use BatchProcessor\DataStructures\LinkedList;

/**
 * Financial transaction batch processor.
 */
final class BatchProcessor
{
    // Initial balance granted to every account that appears in the data set
    private const INITIAL_BALANCE = 10_000;

    // Two transactions are considered duplicates if they share the same source,
    // destination and amount, and their timestamps differ by less than 5 seconds.
    private const DUPLICATE_WINDOW_MS = 5_000;

    // -----------------------------------------------------------------------
    // Public entry point -- runs all 5 stages sequentially
    // -----------------------------------------------------------------------

    /**
     * @param Transaction[] $transactions
     */
    public function process(array $transactions): ProcessingResult
    {
        $total = count($transactions);

        // Stage 1: Duplicate detection
        $s1 = microtime(true);
        [$unique, $duplicateCount] = $this->detectDuplicates($transactions);
        echo "\r  [1/5] Detectando duplicatas... 100% ({$total}/{$total}) "
            . sprintf('%.3f', microtime(true) - $s1) . "s\n";

        // Stage 2: Account balance validation
        $s2 = microtime(true);
        $uniqueCount = count($unique);
        [$accepted, $rejectedCount] = $this->validateBalances($unique);
        echo "\r  [2/5] Validando saldos... 100% ({$uniqueCount}/{$uniqueCount}) "
            . sprintf('%.3f', microtime(true) - $s2) . "s\n";

        // Stage 3: Fraud ring detection
        $s3 = microtime(true);
        $fraudRings = $this->detectFraudRings($accepted);
        echo "\r  [3/5] Detectando aneis de fraude... 100% "
            . sprintf('%.3f', microtime(true) - $s3) . "s\n";

        // Stage 4: Priority sorting
        $s4 = microtime(true);
        $acceptedCount = count($accepted);
        $sorted = $this->sortByPriority($accepted);
        echo "\r  [4/5] Ordenando por prioridade... 100% ({$acceptedCount}/{$acceptedCount}) "
            . sprintf('%.3f', microtime(true) - $s4) . "s\n";

        // Stage 5: Category report
        $s5 = microtime(true);
        $report = $this->generateReport($sorted);
        echo "\r  [5/5] Gerando relatorio por categoria... 100% "
            . sprintf('%.3f', microtime(true) - $s5) . "s\n";

        return new ProcessingResult(
            processed: count($accepted),
            duplicates: $duplicateCount,
            rejected: $rejectedCount,
            fraudRings: $fraudRings,
            report: $report,
        );
    }

    // -----------------------------------------------------------------------
    // Stage 1 -- Duplicate Detection
    // -----------------------------------------------------------------------

    /**
     * @param Transaction[] $transactions
     * @return array{Transaction[], int} [unique, duplicateCount]
     */
    private function detectDuplicates(array $transactions): array
    {
        $seen = new LinkedList();
        $unique = [];
        $duplicateCount = 0;

        $total = count($transactions);
        $interval = max(intdiv($total, 20), 1);

        for ($i = 0; $i < $total; $i++) {
            if ($i % $interval === 0) {
                echo "\r  [1/5] Detectando duplicatas... "
                    . intdiv($i * 100, $total) . "% ({$i}/{$total})";
                flush();
            }

            $tx = $transactions[$i];

            // O(n) scan of the LinkedList for each transaction
            $isDup = $seen->find(
                function ($prev) use ($tx): bool {
                    return $prev->sourceAccount === $tx->sourceAccount
                        && $prev->destAccount === $tx->destAccount
                        && $prev->amount === $tx->amount
                        && abs($prev->timestamp - $tx->timestamp) < self::DUPLICATE_WINDOW_MS;
                }
            );

            if ($isDup !== null) {
                $duplicateCount++;
            } else {
                $unique[] = $tx;
                $seen->append($tx);
            }
        }

        return [$unique, $duplicateCount];
    }

    // -----------------------------------------------------------------------
    // Stage 2 -- Account Balance Validation
    // -----------------------------------------------------------------------

    /**
     * @param Transaction[] $transactions
     * @return array{Transaction[], int} [accepted, rejectedCount]
     */
    private function validateBalances(array $transactions): array
    {
        $balances = new LinkedList();
        $accepted = [];
        $rejectedCount = 0;

        $total = count($transactions);
        $interval = max(intdiv($total, 20), 1);

        for ($i = 0; $i < $total; $i++) {
            if ($i % $interval === 0) {
                echo "\r  [2/5] Validando saldos... "
                    . intdiv($i * 100, $total) . "% ({$i}/{$total})";
                flush();
            }

            $tx = $transactions[$i];

            // Ensure both accounts exist (O(n) find + O(n) insertSorted each)
            $this->ensureAccount($balances, $tx->sourceAccount);
            $this->ensureAccount($balances, $tx->destAccount);

            // Check source has sufficient funds -- O(n) scan
            /** @var AccountBalance|null $sourceEntry */
            $sourceEntry = $balances->find(
                fn(AccountBalance $b): bool => $b->accountId === $tx->sourceAccount
            );

            if ($sourceEntry === null || $sourceEntry->balance < $tx->amount) {
                $rejectedCount++;
                continue;
            }

            // Debit source (mutates in-place since AccountBalance is a class/reference)
            $sourceEntry->balance -= $tx->amount;

            // Credit destination -- O(n) scan
            /** @var AccountBalance|null $destEntry */
            $destEntry = $balances->find(
                fn(AccountBalance $b): bool => $b->accountId === $tx->destAccount
            );
            if ($destEntry !== null) {
                $destEntry->balance += $tx->amount;
            }

            $accepted[] = $tx;
        }

        return [$accepted, $rejectedCount];
    }

    /** Lazily initialise an account with INITIAL_BALANCE (sorted insert, O(n)). */
    private function ensureAccount(LinkedList $balances, string $accountId): void
    {
        $existing = $balances->find(
            fn(AccountBalance $b): bool => $b->accountId === $accountId
        );
        if ($existing === null) {
            $balances->insertSorted(
                new AccountBalance($accountId, self::INITIAL_BALANCE),
                fn(AccountBalance $a, AccountBalance $b): int => strcmp($a->accountId, $b->accountId)
            );
        }
    }

    // -----------------------------------------------------------------------
    // Stage 3 -- Fraud Ring Detection
    // -----------------------------------------------------------------------

    /**
     * @param Transaction[] $transactions
     * @return int number of fraud rings detected
     */
    private function detectFraudRings(array $transactions): int
    {
        if (count($transactions) === 0) {
            return 0;
        }

        // Collect unique accounts
        $accountSet = [];
        foreach ($transactions as $tx) {
            $accountSet[$tx->sourceAccount] = true;
            $accountSet[$tx->destAccount] = true;
        }
        $accounts = array_keys($accountSet);

        // Build adjacency matrix -- O(V^2) allocation
        $graph = new AdjacencyMatrix($accounts);
        foreach ($transactions as $tx) {
            $graph->addEdge($tx->sourceAccount, $tx->destAccount);
        }

        $ringCount = 0;
        $globalVisited = [];
        $totalV = count($accounts);
        $intervalV = max(intdiv($totalV, 20), 1);

        // DFS from every node -- O(V * (V+E)) total
        for ($vi = 0; $vi < $totalV; $vi++) {
            if ($vi % $intervalV === 0) {
                echo "\r  [3/5] Detectando aneis de fraude... "
                    . intdiv($vi * 100, $totalV) . "% ({$vi}/{$totalV})";
                flush();
            }

            $startNode = $accounts[$vi];
            if (isset($globalVisited[$startNode])) {
                continue;
            }

            $visited = [];
            $path = [];
            $ringsFoundInThisDfs = [];

            $this->dfs($graph, $startNode, $visited, $path, $ringsFoundInThisDfs, $globalVisited, $ringCount);
        }

        return $ringCount;
    }

    /**
     * Recursive DFS looking for cycles.
     */
    private function dfs(
        AdjacencyMatrix $graph,
        string          $node,
        array           &$visited,
        array           &$path,
        array           &$ringsFoundInThisDfs,
        array           &$globalVisited,
        int             &$ringCount,
    ): void {
        $visited[$node] = true;
        $path[] = $node;

        // O(V) scan of the matrix row for this node
        $neighbours = $graph->getNeighbors($node);
        foreach ($neighbours as $neighbour) {
            if (!isset($visited[$neighbour])) {
                $this->dfs($graph, $neighbour, $visited, $path, $ringsFoundInThisDfs, $globalVisited, $ringCount);
            } else {
                // Found a back-edge -> cycle detected
                $cycleStart = array_search($neighbour, $path, true);
                if ($cycleStart !== false) {
                    $cycle = array_slice($path, $cycleStart);
                    $sortedCycle = $cycle;
                    sort($sortedCycle);
                    $cycleKey = implode(',', $sortedCycle);
                    if (!isset($ringsFoundInThisDfs[$cycleKey])) {
                        $ringsFoundInThisDfs[$cycleKey] = true;
                        $ringCount++;
                    }
                }
            }
        }

        array_pop($path);
        $globalVisited[$node] = true;
    }

    // -----------------------------------------------------------------------
    // Stage 4 -- Priority Sorting
    // -----------------------------------------------------------------------

    /**
     * @param Transaction[] $transactions
     * @return Transaction[]
     */
    private function sortByPriority(array $transactions): array
    {
        $sorted = new LinkedList();
        $total = count($transactions);
        $interval = max(intdiv($total, 20), 1);

        for ($i = 0; $i < $total; $i++) {
            if ($i % $interval === 0) {
                echo "\r  [4/5] Ordenando por prioridade... "
                    . intdiv($i * 100, $total) . "% ({$i}/{$total})";
                flush();
            }
            // Higher priority number = processed first (descending sort)
            $sorted->insertSorted(
                $transactions[$i],
                fn(Transaction $a, Transaction $b): int => $b->priority - $a->priority,
            );
        }

        return $sorted->toArray();
    }

    // -----------------------------------------------------------------------
    // Stage 5 -- Category Report
    // -----------------------------------------------------------------------

    /**
     * @param Transaction[] $transactions
     * @return CategoryStats[]
     */
    private function generateReport(array $transactions): array
    {
        if (count($transactions) === 0) {
            return [];
        }

        // Collect unique categories with a linear scan, then sort
        $uniqueCategories = [];
        foreach ($transactions as $tx) {
            $uniqueCategories[$tx->category] = true;
        }
        $categories = array_keys($uniqueCategories);
        sort($categories);

        $stats = [];
        $totalCat = count($categories);

        // For each category, do a full second pass -- O(n) per category
        for ($ci = 0; $ci < $totalCat; $ci++) {
            echo "\r  [5/5] Gerando relatorio por categoria... "
                . intdiv($ci * 100, $totalCat) . "% ({$ci}/{$totalCat})";
            flush();

            $cat = $categories[$ci];
            $filtered = array_filter(
                $transactions,
                fn(Transaction $t): bool => $t->category === $cat,
            );

            $total = 0.0;
            foreach ($filtered as $t) {
                $total += $t->amount;
            }
            $count = count($filtered);

            $stats[] = new CategoryStats(
                category: $cat,
                count: $count,
                total: round($total, 2),
                average: round($total / $count, 2),
            );
        }

        return $stats;
    }
}

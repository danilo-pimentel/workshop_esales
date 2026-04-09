<?php

declare(strict_types=1);

namespace BatchProcessor\Tests;

use BatchProcessor\BatchProcessor;
use BatchProcessor\Transaction;
use PHPUnit\Framework\TestCase;

require_once __DIR__ . '/../src/DataGenerator.php';

/**
 * Performance tests for BatchProcessor.
 *
 * Uses the DataGenerator's mulberry32-based generation to produce
 * deterministic datasets, then processes them and verifies counts.
 */
final class PerformanceTest extends TestCase
{
    // -- Helpers --------------------------------------------------------------

    /**
     * Parse CSV text from generateTransactionsCsv() into Transaction objects.
     *
     * @return Transaction[]
     */
    private function parseCsv(string $csvText): array
    {
        $lines = explode("\n", trim($csvText));
        $transactions = [];

        foreach ($lines as $line) {
            $trimmed = trim($line);
            if ($trimmed === '' || str_starts_with($trimmed, 'id,')) {
                continue;
            }
            $transactions[] = Transaction::fromCsvRow($trimmed);
        }

        return $transactions;
    }

    // -- Tests ----------------------------------------------------------------

    /**
     * @group performance
     */
    public function testProcesses1kRecords(): void
    {
        $csvText = \generateTransactionsCsv(1000);
        $transactions = $this->parseCsv($csvText);

        $processor = new BatchProcessor();
        $start = microtime(true);
        $result = $processor->process($transactions);
        $elapsed = microtime(true) - $start;

        $total = $result->processed + $result->duplicates + $result->rejected;
        $this->assertSame(1000, $total,
            'processed + duplicates + rejected must equal 1000');

        fwrite(STDOUT, sprintf(
            "\n[PERF] 1k records: %.4f s  processed=%d  dups=%d  rejected=%d  rings=%d\n",
            $elapsed,
            $result->processed,
            $result->duplicates,
            $result->rejected,
            $result->fraudRings,
        ));

        $this->assertLessThan(60.0, $elapsed,
            '1000 transactions took over 60 s');
    }

    /**
     * @group performance
     * @group slow
     */
    public function testProcesses10kRecords(): void
    {
        $csvText = \generateTransactionsCsv(10000);
        $transactions = $this->parseCsv($csvText);

        $processor = new BatchProcessor();
        $start = microtime(true);
        $result = $processor->process($transactions);
        $elapsed = microtime(true) - $start;

        $total = $result->processed + $result->duplicates + $result->rejected;
        $this->assertSame(10000, $total,
            'processed + duplicates + rejected must equal 10000');

        fwrite(STDOUT, sprintf(
            "\n[PERF] 10k records: %.4f s  processed=%d  dups=%d  rejected=%d  rings=%d\n",
            $elapsed,
            $result->processed,
            $result->duplicates,
            $result->rejected,
            $result->fraudRings,
        ));

        // Log category report
        fwrite(STDOUT, "[PERF] Category report:\n");
        foreach ($result->report as $stat) {
            fwrite(STDOUT, sprintf(
                "  %-12s  count=%d  total=%.2f  avg=%.2f\n",
                $stat->category,
                $stat->count,
                $stat->total,
                $stat->average,
            ));
        }

        $this->assertLessThan(600.0, $elapsed,
            '10000 transactions took over 600 s');
    }

    /**
     * @group performance
     */
    public function testScalingRatio(): void
    {
        // Build two datasets of different sizes
        $csv500 = \generateTransactionsCsv(500);
        $tx500 = $this->parseCsv($csv500);

        $csv1000 = \generateTransactionsCsv(1000);
        $tx1000 = $this->parseCsv($csv1000);

        // Time the 500-record batch
        $start500 = microtime(true);
        (new BatchProcessor())->process($tx500);
        $time500 = microtime(true) - $start500;

        // Time the 1000-record batch
        $start1000 = microtime(true);
        (new BatchProcessor())->process($tx1000);
        $time1000 = microtime(true) - $start1000;

        $ratio = $time500 > 0 ? $time1000 / $time500 : 0.0;

        fwrite(STDOUT, sprintf(
            "\n[PERF] Scaling: 500 tx = %.4f s, 1000 tx = %.4f s, ratio = %.2fx  (O(n^2) predicts ~4x)\n",
            $time500,
            $time1000,
            $ratio,
        ));

        // Basic sanity: larger batch should take at least as long
        $this->assertGreaterThanOrEqual(
            $time500,
            $time1000 + 0.0001,
            'Larger batch should take at least as long as the smaller one',
        );
    }
}

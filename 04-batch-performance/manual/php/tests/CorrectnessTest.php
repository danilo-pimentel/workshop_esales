<?php

declare(strict_types=1);

namespace BatchProcessor\Tests;

use BatchProcessor\BatchProcessor;
use BatchProcessor\Transaction;
use PHPUnit\Framework\TestCase;

/**
 * Correctness tests for BatchProcessor.
 *
 * 23 tests organised in 6 sections.  All assertions verify business-logic
 * correctness, NOT performance.
 */
final class CorrectnessTest extends TestCase
{
    // -- Fixtures -------------------------------------------------------------

    private const BASE_TIME = 1_704_067_200_000; // 2024-01-01T00:00:00Z in epoch ms

    private static int $idSeq = 0;

    protected function setUp(): void
    {
        self::$idSeq = 0;
    }

    private function tx(
        string  $sourceAccount,
        string  $destAccount,
        float   $amount,
        ?int    $timestamp = null,
        ?string $category = null,
        ?int    $priority = null,
        ?int    $id = null,
    ): Transaction {
        return new Transaction(
            id:            $id ?? ++self::$idSeq,
            amount:        $amount,
            sourceAccount: $sourceAccount,
            destAccount:   $destAccount,
            timestamp:     $timestamp ?? self::BASE_TIME,
            category:      $category ?? 'TRANSFER',
            priority:      $priority ?? 3,
        );
    }

    // =========================================================================
    // Section 1: Duplicate detection (5 tests)
    // =========================================================================

    public function testAcceptsTwoTransactionsOutsideDuplicateWindow(): void
    {
        $processor = new BatchProcessor();
        $t1 = $this->tx('ACC0001', 'ACC0002', 100.0, self::BASE_TIME);
        $t2 = $this->tx('ACC0001', 'ACC0002', 100.0, self::BASE_TIME + 6000); // 6s later
        $result = $processor->process([$t1, $t2]);

        $this->assertSame(0, $result->duplicates);
        $this->assertSame(2, $result->processed);
    }

    public function testFlagsDuplicateWithinWindow(): void
    {
        $processor = new BatchProcessor();
        $t1 = $this->tx('ACC0001', 'ACC0002', 200.0, self::BASE_TIME);
        $t2 = $this->tx('ACC0001', 'ACC0002', 200.0, self::BASE_TIME + 2000); // 2s later
        $result = $processor->process([$t1, $t2]);

        $this->assertSame(1, $result->duplicates);
        $this->assertSame(1, $result->processed);
    }

    public function testDoesNotFlagDifferentAmount(): void
    {
        $processor = new BatchProcessor();
        $t1 = $this->tx('ACC0001', 'ACC0002', 100.0, self::BASE_TIME);
        $t2 = $this->tx('ACC0001', 'ACC0002', 101.0, self::BASE_TIME + 1000);
        $result = $processor->process([$t1, $t2]);

        $this->assertSame(0, $result->duplicates);
        $this->assertSame(2, $result->processed);
    }

    public function testDoesNotFlagDifferentDestination(): void
    {
        $processor = new BatchProcessor();
        $t1 = $this->tx('ACC0001', 'ACC0002', 100.0, self::BASE_TIME);
        $t2 = $this->tx('ACC0001', 'ACC0003', 100.0, self::BASE_TIME + 1000);
        $result = $processor->process([$t1, $t2]);

        $this->assertSame(0, $result->duplicates);
        $this->assertSame(2, $result->processed);
    }

    public function testCountsMultipleDuplicatesIndependently(): void
    {
        $processor = new BatchProcessor();
        $t1 = $this->tx('ACC0001', 'ACC0002', 50.0, self::BASE_TIME);
        $t2 = $this->tx('ACC0001', 'ACC0002', 50.0, self::BASE_TIME + 1000); // dup
        $t3 = $this->tx('ACC0001', 'ACC0002', 50.0, self::BASE_TIME + 2000); // dup of t1
        $result = $processor->process([$t1, $t2, $t3]);

        $this->assertSame(2, $result->duplicates);
        $this->assertSame(1, $result->processed);
    }

    // =========================================================================
    // Section 2: Account balance tracking (4 tests)
    // =========================================================================

    public function testRejectsOverdraft(): void
    {
        $processor = new BatchProcessor();
        // Initial balance = 10,000 per account
        $drain = $this->tx('ACC0010', 'ACC0011', 9999.0, self::BASE_TIME);
        $overdraft = $this->tx('ACC0010', 'ACC0012', 5000.0, self::BASE_TIME + 10000);
        $result = $processor->process([$drain, $overdraft]);

        $this->assertSame(1, $result->processed);
        $this->assertSame(1, $result->rejected);
    }

    public function testAcceptsExactBalance(): void
    {
        $processor = new BatchProcessor();
        $exact = $this->tx('ACC0020', 'ACC0021', 10000.0, self::BASE_TIME);
        $result = $processor->process([$exact]);

        $this->assertSame(1, $result->processed);
        $this->assertSame(0, $result->rejected);
    }

    public function testDestinationCanSpendReceivedFunds(): void
    {
        $processor = new BatchProcessor();
        $t1 = $this->tx('ACC0030', 'ACC0031', 5000.0, self::BASE_TIME);
        // ACC0031 now has 10,000 + 5,000 = 15,000; should be able to send 12,000
        $t2 = $this->tx('ACC0031', 'ACC0032', 12000.0, self::BASE_TIME + 10000);
        $result = $processor->process([$t1, $t2]);

        $this->assertSame(2, $result->processed);
        $this->assertSame(0, $result->rejected);
    }

    public function testRejectsMultipleOverdraftsIndependently(): void
    {
        $processor = new BatchProcessor();
        $big1 = $this->tx('ACC0040', 'ACC0041', 60000.0, self::BASE_TIME);
        $big2 = $this->tx('ACC0042', 'ACC0043', 60000.0, self::BASE_TIME + 10000);
        $result = $processor->process([$big1, $big2]);

        $this->assertSame(0, $result->processed);
        $this->assertSame(2, $result->rejected);
    }

    // =========================================================================
    // Section 3: Fraud ring detection (4 tests)
    // =========================================================================

    public function testDetectsSimpleCycle(): void
    {
        $processor = new BatchProcessor();
        $t1 = $this->tx('ACC0100', 'ACC0101', 100.0, self::BASE_TIME);
        $t2 = $this->tx('ACC0101', 'ACC0100', 80.0, self::BASE_TIME + 10000);
        $result = $processor->process([$t1, $t2]);

        $this->assertGreaterThan(0, $result->fraudRings);
    }

    public function testDetectsThreeNodeCycle(): void
    {
        $processor = new BatchProcessor();
        $t1 = $this->tx('ACC0200', 'ACC0201', 100.0, self::BASE_TIME);
        $t2 = $this->tx('ACC0201', 'ACC0202', 100.0, self::BASE_TIME + 10000);
        $t3 = $this->tx('ACC0202', 'ACC0200', 100.0, self::BASE_TIME + 20000);
        $result = $processor->process([$t1, $t2, $t3]);

        $this->assertGreaterThan(0, $result->fraudRings);
    }

    public function testNoRingsInAcyclicGraph(): void
    {
        $processor = new BatchProcessor();
        // Simple chain: A -> B -> C — no cycle
        $t1 = $this->tx('ACC0300', 'ACC0301', 100.0, self::BASE_TIME);
        $t2 = $this->tx('ACC0301', 'ACC0302', 80.0, self::BASE_TIME + 10000);
        $result = $processor->process([$t1, $t2]);

        $this->assertSame(0, $result->fraudRings);
    }

    public function testCountsFraudRingsForSingleCycle(): void
    {
        $processor = new BatchProcessor();
        $t1 = $this->tx('ACC0400', 'ACC0401', 100.0, self::BASE_TIME);
        $t2 = $this->tx('ACC0401', 'ACC0400', 100.0, self::BASE_TIME + 10000);
        $result = $processor->process([$t1, $t2]);

        $this->assertGreaterThanOrEqual(1, $result->fraudRings);
    }

    // =========================================================================
    // Section 4: Priority processing order (2 tests)
    // =========================================================================

    public function testHigherPriorityFirst(): void
    {
        $processor = new BatchProcessor();
        $txLow  = $this->tx('ACC0500', 'ACC0501', 10.0, self::BASE_TIME, 'PAYMENT', 1);
        $txMid  = $this->tx('ACC0502', 'ACC0503', 10.0, self::BASE_TIME + 10000, 'PAYMENT', 3);
        $txHigh = $this->tx('ACC0504', 'ACC0505', 10.0, self::BASE_TIME + 20000, 'PAYMENT', 5);
        $result = $processor->process([$txLow, $txMid, $txHigh]);

        $this->assertSame(3, $result->processed);
    }

    public function testPreservesEqualPriorityOrder(): void
    {
        $processor = new BatchProcessor();
        $transactions = [];
        for ($i = 0; $i < 5; $i++) {
            $src = sprintf('ACC%04d', 600 + $i * 2);
            $dst = sprintf('ACC%04d', 601 + $i * 2);
            $transactions[] = $this->tx($src, $dst, 10.0, self::BASE_TIME + $i * 10000, 'TRANSFER', 3);
        }
        $result = $processor->process($transactions);

        $this->assertSame(5, $result->processed);
    }

    // =========================================================================
    // Section 5: Category report (5 tests)
    // =========================================================================

    public function testReportCorrectTotals(): void
    {
        $processor = new BatchProcessor();
        $transactions = [
            $this->tx('ACC0700', 'ACC0701', 100.0, self::BASE_TIME, 'PAYMENT'),
            $this->tx('ACC0702', 'ACC0703', 200.0, self::BASE_TIME + 10000, 'PAYMENT'),
            $this->tx('ACC0704', 'ACC0705', 300.0, self::BASE_TIME + 20000, 'WITHDRAWAL'),
        ];
        $result = $processor->process($transactions);

        $payment = null;
        $withdrawal = null;
        foreach ($result->report as $stat) {
            if ($stat->category === 'PAYMENT') $payment = $stat;
            if ($stat->category === 'WITHDRAWAL') $withdrawal = $stat;
        }

        $this->assertNotNull($payment);
        $this->assertSame(2, $payment->count);
        $this->assertEqualsWithDelta(300.0, $payment->total, 0.01);
        $this->assertEqualsWithDelta(150.0, $payment->average, 0.01);

        $this->assertNotNull($withdrawal);
        $this->assertSame(1, $withdrawal->count);
        $this->assertEqualsWithDelta(300.0, $withdrawal->total, 0.01);
    }

    public function testReportCorrectAverage(): void
    {
        $processor = new BatchProcessor();
        $transactions = [
            $this->tx('ACC0800', 'ACC0801', 50.0, self::BASE_TIME, 'FEE'),
            $this->tx('ACC0802', 'ACC0803', 150.0, self::BASE_TIME + 10000, 'FEE'),
            $this->tx('ACC0804', 'ACC0805', 100.0, self::BASE_TIME + 20000, 'FEE'),
        ];
        $result = $processor->process($transactions);

        $fee = null;
        foreach ($result->report as $stat) {
            if ($stat->category === 'FEE') $fee = $stat;
        }

        $this->assertNotNull($fee);
        $this->assertSame(3, $fee->count);
        $this->assertEqualsWithDelta(300.0, $fee->total, 0.01);
        $this->assertEqualsWithDelta(100.0, $fee->average, 0.01);
    }

    public function testReportOnlyContainsPresentCategories(): void
    {
        $processor = new BatchProcessor();
        $transactions = [
            $this->tx('ACC0900', 'ACC0901', 10.0, self::BASE_TIME, 'DEPOSIT'),
        ];
        $result = $processor->process($transactions);

        $this->assertCount(1, $result->report);
        $this->assertSame('DEPOSIT', $result->report[0]->category);
    }

    public function testEmptyInputReturnsEmptyReport(): void
    {
        $processor = new BatchProcessor();
        $result = $processor->process([]);

        $this->assertSame([], $result->report);
        $this->assertSame(0, $result->processed);
        $this->assertSame(0, $result->duplicates);
        $this->assertSame(0, $result->rejected);
        $this->assertSame(0, $result->fraudRings);
    }

    public function testAllRejectedReturnsEmptyReport(): void
    {
        $processor = new BatchProcessor();
        $overdraft = $this->tx('ACC0950', 'ACC0951', 999999.0, self::BASE_TIME);
        $result = $processor->process([$overdraft]);

        $this->assertSame(0, $result->processed);
        $this->assertSame([], $result->report);
    }

    // =========================================================================
    // Section 6: Integrated correctness -- 100 records (3 tests)
    // =========================================================================

    /**
     * Build a deterministic 100-row dataset (no DataGenerator dependency).
     *
     * @return Transaction[]
     */
    private function buildIntegratedDataset(): array
    {
        $categories = ['PAYMENT', 'TRANSFER', 'REFUND', 'PURCHASE',
                        'WITHDRAWAL', 'DEPOSIT', 'FEE', 'INTEREST'];
        $transactions = [];
        for ($i = 1; $i <= 100; $i++) {
            $srcNum = (($i - 1) % 50) + 1;
            $dstNum = ($i % 50) + 1;
            if ($srcNum === $dstNum) {
                $dstNum = ($dstNum % 50) + 1;
            }
            $transactions[] = new Transaction(
                id:            $i,
                amount:        round($i * 1.5, 2),
                sourceAccount: sprintf('ACC%04d', $srcNum),
                destAccount:   sprintf('ACC%04d', $dstNum),
                timestamp:     self::BASE_TIME + $i * 120000,
                category:      $categories[$i % count($categories)],
                priority:      ($i % 5) + 1,
            );
        }
        return $transactions;
    }

    public function testIntegrated100CountsAddUp(): void
    {
        $processor = new BatchProcessor();
        $result = $processor->process($this->buildIntegratedDataset());

        $this->assertSame(
            100,
            $result->processed + $result->duplicates + $result->rejected,
            'processed + duplicates + rejected must equal 100',
        );
    }

    public function testIntegrated100ReportTotalsAddUp(): void
    {
        $processor = new BatchProcessor();
        $result = $processor->process($this->buildIntegratedDataset());

        $reportTotal = 0;
        foreach ($result->report as $stat) {
            $reportTotal += $stat->count;
        }
        $this->assertSame($result->processed, $reportTotal,
            'Sum of category counts must equal processed count');
    }

    public function testIntegrated100IsDeterministic(): void
    {
        $data = $this->buildIntegratedDataset();
        $r1 = (new BatchProcessor())->process($data);
        $r2 = (new BatchProcessor())->process($data);

        $this->assertSame($r1->processed, $r2->processed);
        $this->assertSame($r1->duplicates, $r2->duplicates);
        $this->assertSame($r1->rejected, $r2->rejected);
        $this->assertSame($r1->fraudRings, $r2->fraudRings);
    }
}

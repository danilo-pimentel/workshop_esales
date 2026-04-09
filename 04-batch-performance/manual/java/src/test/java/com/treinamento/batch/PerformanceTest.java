package com.treinamento.batch;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance regression tests for the batch processor.
 *
 * <p>3 tests:
 * <ol>
 *   <li>1,000 records — warm-up, must complete under 10s</li>
 *   <li>10,000 records — main benchmark, must complete under 120s</li>
 *   <li>Scaling ratio — 500 vs 1,000 records, demonstrates O(n^2) behaviour</li>
 * </ol>
 *
 * <h2>Expected behaviour before optimisation</h2>
 * <pre>
 *   1,000 transactions   ->  near-instant
 *   10,000 transactions  ->  ~20-60 seconds (depends on hardware)
 * </pre>
 *
 * <h2>Goal after optimisation</h2>
 * <pre>
 *   10,000 transactions  ->  &lt; 0.5s
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceTest {

    // =======================================================================
    // 1. Process 1,000 records
    // =======================================================================

    @Test
    @Order(1)
    @DisplayName("Processes 1,000 records within 10 seconds")
    void testProcesses1kRecords() throws IOException {
        Path tmpDir = Files.createTempDirectory("batch-perf-1k-");
        Path csvPath = tmpDir.resolve("transactions_1k.csv");
        DataGenerator.generate(1_000, csvPath);

        BatchProcessor processor = new BatchProcessor();

        System.out.printf("%n[PerformanceTest] Starting 1k processing ...%n");
        long t0 = System.currentTimeMillis();

        ProcessingResult result = processor.process(csvPath);

        long elapsed = System.currentTimeMillis() - t0;

        // Verify counts add up
        assertEquals(1_000,
                result.processed() + result.duplicates() + result.rejected(),
                "processed + duplicates + rejected must equal 1,000");

        // Assert time ceiling
        assertTrue(elapsed < 10_000,
                String.format("Processing took %d ms, which exceeds the 10,000 ms ceiling", elapsed));

        // Log timing
        System.out.println();
        System.out.println("========================================");
        System.out.printf(" PERFORMANCE RESULT (1,000 records)%n");
        System.out.println("========================================");
        System.out.printf(" Wall time         : %,d ms%n", elapsed);
        System.out.printf(" Processed         : %,d%n", result.processed());
        System.out.printf(" Duplicates        : %,d%n", result.duplicates());
        System.out.printf(" Rejected          : %,d%n", result.rejected());
        System.out.printf(" Fraud rings       : %,d%n", result.fraudRings());
        System.out.println("========================================");
    }

    // =======================================================================
    // 2. Process 10,000 records
    // =======================================================================

    @Test
    @Order(2)
    @DisplayName("Processes 10,000 records within 120 seconds")
    void testProcesses10kRecords() throws IOException {
        Path tmpDir = Files.createTempDirectory("batch-perf-10k-");
        Path csvPath = tmpDir.resolve("transactions_10k.csv");
        DataGenerator.generate(10_000, csvPath);

        BatchProcessor processor = new BatchProcessor();

        System.out.printf("%n[PerformanceTest] Starting 10k processing ...%n");
        long t0 = System.currentTimeMillis();

        ProcessingResult result = processor.process(csvPath);

        long elapsed = System.currentTimeMillis() - t0;

        // Verify counts add up
        assertEquals(10_000,
                result.processed() + result.duplicates() + result.rejected(),
                "processed + duplicates + rejected must equal 10,000");

        // Assert time ceiling
        assertTrue(elapsed < 120_000,
                String.format("Processing took %d ms, which exceeds the 120,000 ms ceiling", elapsed));

        // Log timing + category report
        System.out.println();
        System.out.println("========================================");
        System.out.printf(" PERFORMANCE RESULT (10,000 records)%n");
        System.out.println("========================================");
        System.out.printf(" Wall time         : %,d ms%n", elapsed);
        System.out.printf(" Processed         : %,d%n", result.processed());
        System.out.printf(" Duplicates        : %,d%n", result.duplicates());
        System.out.printf(" Rejected          : %,d%n", result.rejected());
        System.out.printf(" Fraud rings       : %,d%n", result.fraudRings());
        System.out.printf(" Categories        : %,d%n", result.report().size());
        System.out.println("----------------------------------------");
        System.out.println(" Category report:");
        for (ProcessingResult.CategoryReport cat : result.report()) {
            System.out.printf("   %-16s count=%5d  total=%12.2f%n",
                    cat.category(), cat.count(), cat.total());
        }
        System.out.println("========================================");

        printPerformanceHint(elapsed);
    }

    // =======================================================================
    // 3. Scaling ratio: 500 vs 1,000 records
    // =======================================================================

    @Test
    @Order(3)
    @DisplayName("Demonstrates O(n^2) scaling: 1000 records vs 500 records")
    void testScalingRatio() throws IOException {
        // Generate two datasets with different seeds (different counts act as different seeds)
        Path tmpDir = Files.createTempDirectory("batch-perf-scaling-");

        Path csv500 = tmpDir.resolve("transactions_500.csv");
        DataGenerator.generate(500, csv500);

        Path csv1000 = tmpDir.resolve("transactions_1000.csv");
        DataGenerator.generate(1_000, csv1000);

        // Time 500 records
        long t0 = System.currentTimeMillis();
        new BatchProcessor().process(csv500);
        long time500 = System.currentTimeMillis() - t0;

        // Time 1000 records
        long t1 = System.currentTimeMillis();
        new BatchProcessor().process(csv1000);
        long time1000 = System.currentTimeMillis() - t1;

        double ratio = time1000 > 0 && time500 > 0 ? (double) time1000 / time500 : 0;

        // Log the ratio
        System.out.println();
        System.out.println("========================================");
        System.out.println(" SCALING RATIO (1000 / 500)");
        System.out.println("========================================");
        System.out.printf(" 500  records : %,d ms%n", time500);
        System.out.printf(" 1000 records : %,d ms%n", time1000);
        System.out.printf(" Ratio        : %.2fx%n", ratio);
        System.out.println(" (O(n^2) would be ~4x; O(n log n) would be ~2.2x)");
        System.out.println("========================================");

        // We just verify that the timing is positive — the ratio is informational.
        // Students can observe the ratio trending toward 4x on larger sizes.
        assertTrue(time1000 > 0, "1000-record processing time must be positive");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private static void printPerformanceHint(long elapsed) {
        System.out.println();
        if (elapsed < 2_000) {
            System.out.println("[HINT] Excellent! Processing completed in under 2 seconds.");
            System.out.println("       Are you sure the bottlenecks are still in place?");
        } else if (elapsed < 15_000) {
            System.out.println("[HINT] Good progress! Try to reach < 2 seconds for 10k records.");
        } else if (elapsed < 60_000) {
            System.out.printf("[HINT] Took %.1f seconds. The O(n^2) bottlenecks are visible. "
                    + "The challenge: optimise to < 2 s without changing business logic.%n",
                    elapsed / 1000.0);
        } else {
            System.out.printf("[HINT] Took %.1f seconds -- this is exactly the O(n^2) problem "
                    + "the exercise is about. A 1M-row file would take hours.%n",
                    elapsed / 1000.0);
        }
    }
}

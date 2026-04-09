package com.treinamento.batch

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.nio.file.Files

/**
 * Performance characterisation tests.
 *
 * These tests measure and LOG execution time for increasing dataset sizes.
 * They use lenient ceilings so they always PASS on slow machines, but the
 * logged timing gives developers a baseline for measuring improvements.
 *
 * Run with:
 *   ./gradlew test --tests "com.treinamento.batch.PerformanceTest"
 *
 * Expected baseline (O(n^2) implementation):
 *   1 000 tx  ->  ~500 ms
 *  10 000 tx  -> ~20 000 ms (20 s)
 *
 * After optimisation target:
 *  10 000 tx -> < 500 ms
 */
@DisplayName("Performance Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PerformanceTest {

    // -- Helper ---------------------------------------------------------------

    private fun generateAndLoad(count: Int): List<Transaction> {
        val tmpFile = Files.createTempFile("perf_bench_${count}_", ".csv").toFile()
        try {
            DataGenerator.generate(count, tmpFile)
            return tmpFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("id,") }
                .map { Transaction.fromCsvLine(it) }
        } finally {
            tmpFile.delete()
        }
    }

    // -- Benchmarks -----------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Benchmark: 1 000 transactions")
    fun bench1k() {
        println("\n  BENCHMARK - 1,000 transactions")
        val data = generateAndLoad(1_000)
        val processor = BatchProcessor()

        val t0 = System.nanoTime()
        val result = processor.process(data)
        val elapsed = (System.nanoTime() - t0) / 1_000_000.0

        println("\n  [1k] elapsed: ${"%.1f".format(elapsed)}ms")
        println("       processed=${result.processed}  duplicates=${result.duplicates}  rejected=${result.rejected}  rings=${result.fraudRings}")

        assertEquals(data.size, result.processed + result.duplicates + result.rejected)
        assertTrue(elapsed < 10_000, "1k transactions should complete in under 10 seconds")
    }

    @Test
    @Order(2)
    @DisplayName("Benchmark: 10 000 transactions  <- MAIN TARGET")
    fun bench10k() {
        println("\n  BENCHMARK - 10,000 transactions  <- MAIN TARGET")
        println("  Baseline (slow)  : ~20,000 ms")
        println("  Challenge target : < 500 ms")
        val data = generateAndLoad(10_000)
        val processor = BatchProcessor()

        val t0 = System.nanoTime()
        val result = processor.process(data)
        val elapsed = (System.nanoTime() - t0) / 1_000_000.0

        println("\n  [10k] elapsed: ${"%.1f".format(elapsed)}ms")
        println("        processed=${result.processed}  duplicates=${result.duplicates}  rejected=${result.rejected}  rings=${result.fraudRings}")
        println("\n  Category report:")
        for (stat in result.report) {
            println("    ${stat.category.padEnd(16)} count=${stat.count.toString().padStart(5)}  total=${"%.2f".format(stat.total).padStart(12)}")
        }

        assertEquals(data.size, result.processed + result.duplicates + result.rejected)
        assertTrue(elapsed < 120_000, "10k transactions should complete in under 120 seconds")

        println()
        if (elapsed < 500) {
            println("  *** CHALLENGE COMPLETE! ${"%.0f".format(elapsed)} ms < 500 ms target ***")
        } else {
            println("  Challenge not yet met: ${"%.0f".format(elapsed)} ms (target < 500 ms)")
        }
    }

    @Test
    @Order(3)
    @DisplayName("Scaling ratio: 500 vs 1 000 (expect ~4x for O(n^2))")
    fun scalingRatio() {
        println("\n  SCALING ANALYSIS - 500 vs 1,000 transactions")
        println("  An O(n^2) algorithm produces a ~4x slowdown when n doubles.\n")

        val data500  = generateAndLoad(500)
        val data1000 = generateAndLoad(1_000)

        val t0 = System.nanoTime()
        BatchProcessor().process(data500)
        val time500 = (System.nanoTime() - t0) / 1_000_000.0

        val t1 = System.nanoTime()
        BatchProcessor().process(data1000)
        val time1000 = (System.nanoTime() - t1) / 1_000_000.0

        val ratio = time1000 / maxOf(time500, 0.1)
        println("\n  500  records: ${"%.1f".format(time500)}ms")
        println("  1000 records: ${"%.1f".format(time1000)}ms")
        println("  Scaling ratio (1000 / 500): ${"%.2f".format(ratio)}x")
        println("  (O(n^2) would be ~4x; O(n log n) would be ~2.2x)")

        // We just log the ratio - don't assert a specific value
        assertTrue(time1000 > 0)
    }
}

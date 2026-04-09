package com.treinamento.batch

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * Correctness tests for the batch-processing pipeline.
 *
 * 23 tests organised in 6 sections. All use small hand-crafted datasets
 * so they complete instantly. Performance is NOT the concern here.
 */
@DisplayName("Correctness Tests")
class CorrectnessTest {

    // =========================================================================
    // Helper
    // =========================================================================

    private var idSeq = 0

    @BeforeEach
    fun resetIdSeq() {
        idSeq = 0
    }

    /** Base timestamp: 2024-01-01T00:00:00Z */
    private val BASE_TS = 1704067200000L

    private fun tx(
        sourceAccount: String,
        destAccount: String,
        amount: Double,
        timestamp: Long = BASE_TS,
        category: String = "TRANSFER",
        priority: Int = 3
    ): Transaction {
        return Transaction(
            id = ++idSeq,
            amount = amount,
            sourceAccount = sourceAccount,
            destAccount = destAccount,
            timestamp = timestamp,
            category = category,
            priority = priority
        )
    }

    // =========================================================================
    // 1. Duplicate detection (5 tests)
    // =========================================================================

    @Nested
    @DisplayName("Duplicate detection")
    inner class DuplicateDetectionTests {

        @Test
        @DisplayName("accepts two transactions with same fields but >5s apart")
        fun notDuplicateOutsideWindow() {
            val processor = BatchProcessor()
            val t1 = tx("ACC0001", "ACC0002", 100.0, timestamp = BASE_TS)
            val t2 = tx("ACC0001", "ACC0002", 100.0, timestamp = BASE_TS + 6_000) // 6s later
            val result = processor.process(listOf(t1, t2))
            assertEquals(0, result.duplicates)
            assertEquals(2, result.processed)
        }

        @Test
        @DisplayName("flags a transaction as duplicate when fields match within 5 seconds")
        fun duplicateWithinWindow() {
            val processor = BatchProcessor()
            val t1 = tx("ACC0001", "ACC0002", 200.0, timestamp = BASE_TS)
            val t2 = tx("ACC0001", "ACC0002", 200.0, timestamp = BASE_TS + 2_000) // 2s later
            val result = processor.process(listOf(t1, t2))
            assertEquals(1, result.duplicates)
            assertEquals(1, result.processed)
        }

        @Test
        @DisplayName("does not flag as duplicate when amounts differ")
        fun notDuplicateDifferentAmount() {
            val processor = BatchProcessor()
            val t1 = tx("ACC0001", "ACC0002", 100.0, timestamp = BASE_TS)
            val t2 = tx("ACC0001", "ACC0002", 101.0, timestamp = BASE_TS + 1_000)
            val result = processor.process(listOf(t1, t2))
            assertEquals(0, result.duplicates)
            assertEquals(2, result.processed)
        }

        @Test
        @DisplayName("does not flag as duplicate when destination differs")
        fun notDuplicateDifferentDest() {
            val processor = BatchProcessor()
            val t1 = tx("ACC0001", "ACC0002", 100.0, timestamp = BASE_TS)
            val t2 = tx("ACC0001", "ACC0003", 100.0, timestamp = BASE_TS + 1_000)
            val result = processor.process(listOf(t1, t2))
            assertEquals(0, result.duplicates)
            assertEquals(2, result.processed)
        }

        @Test
        @DisplayName("counts multiple duplicates independently")
        fun multipleDuplicates() {
            val processor = BatchProcessor()
            val t1 = tx("ACC0001", "ACC0002", 50.0, timestamp = BASE_TS)
            val t2 = tx("ACC0001", "ACC0002", 50.0, timestamp = BASE_TS + 1_000) // dup
            val t3 = tx("ACC0001", "ACC0002", 50.0, timestamp = BASE_TS + 2_000) // dup
            val result = processor.process(listOf(t1, t2, t3))
            assertEquals(2, result.duplicates)
            assertEquals(1, result.processed)
        }
    }

    // =========================================================================
    // 2. Account balance tracking (4 tests)
    // =========================================================================

    @Nested
    @DisplayName("Account balance tracking")
    inner class BalanceTests {

        @Test
        @DisplayName("rejects a transaction when source has insufficient funds")
        fun insufficientFunds() {
            val processor = BatchProcessor()
            // Initial balance = 10,000 per account
            val drain = tx("ACC0010", "ACC0011", 9_999.0)
            val overdraft = tx("ACC0010", "ACC0012", 5_000.0)
            val result = processor.process(listOf(drain, overdraft))
            assertEquals(1, result.processed)
            assertEquals(1, result.rejected)
        }

        @Test
        @DisplayName("accepts a transaction when source has exactly enough funds")
        fun exactBalance() {
            val processor = BatchProcessor()
            val exact = tx("ACC0020", "ACC0021", 10_000.0)
            val result = processor.process(listOf(exact))
            assertEquals(1, result.processed)
            assertEquals(0, result.rejected)
        }

        @Test
        @DisplayName("allows destination to spend funds it received earlier")
        fun receivedFundsCanBeSpent() {
            val processor = BatchProcessor()
            val t1 = tx("ACC0030", "ACC0031", 5_000.0)
            // ACC0031 now has 15,000 (10,000 initial + 5,000 received); can send 12,000
            val t2 = tx("ACC0031", "ACC0032", 12_000.0)
            val result = processor.process(listOf(t1, t2))
            assertEquals(2, result.processed)
            assertEquals(0, result.rejected)
        }

        @Test
        @DisplayName("rejects multiple overdraft attempts independently")
        fun multipleOverdrafts() {
            val processor = BatchProcessor()
            val big1 = tx("ACC0040", "ACC0041", 60_000.0)
            val big2 = tx("ACC0042", "ACC0043", 60_000.0)
            val result = processor.process(listOf(big1, big2))
            assertEquals(0, result.processed)
            assertEquals(2, result.rejected)
        }
    }

    // =========================================================================
    // 3. Fraud ring detection (4 tests)
    // =========================================================================

    @Nested
    @DisplayName("Fraud ring detection")
    inner class FraudRingTests {

        @Test
        @DisplayName("detects a simple A->B->A cycle")
        fun detectSimpleCycle() {
            val processor = BatchProcessor()
            val t1 = tx("ACC0100", "ACC0101", 100.0)
            val t2 = tx("ACC0101", "ACC0100", 80.0)
            val result = processor.process(listOf(t1, t2))
            assertTrue(result.fraudRings > 0)
        }

        @Test
        @DisplayName("detects a three-node cycle A->B->C->A")
        fun detectThreeNodeCycle() {
            val processor = BatchProcessor()
            val t1 = tx("ACC0200", "ACC0201", 100.0)
            val t2 = tx("ACC0201", "ACC0202", 100.0)
            val t3 = tx("ACC0202", "ACC0200", 100.0)
            val result = processor.process(listOf(t1, t2, t3))
            assertTrue(result.fraudRings > 0)
        }

        @Test
        @DisplayName("returns no rings when graph is acyclic")
        fun noRingsInAcyclicGraph() {
            val processor = BatchProcessor()
            // Simple chain: A->B->C - no cycle
            val t1 = tx("ACC0300", "ACC0301", 100.0)
            val t2 = tx("ACC0301", "ACC0302", 80.0)
            val result = processor.process(listOf(t1, t2))
            assertEquals(0, result.fraudRings)
        }

        @Test
        @DisplayName("counts at least one fraud ring for a single cycle")
        fun countsFraudRingsForSingleCycle() {
            val processor = BatchProcessor()
            val t1 = tx("ACC0400", "ACC0401", 100.0)
            val t2 = tx("ACC0401", "ACC0400", 100.0)
            val result = processor.process(listOf(t1, t2))
            assertTrue(result.fraudRings >= 1)
        }
    }

    // =========================================================================
    // 4. Priority processing order (2 tests)
    // =========================================================================

    @Nested
    @DisplayName("Priority processing order")
    inner class PriorityTests {

        @Test
        @DisplayName("higher-priority transactions are processed first")
        fun higherPriorityFirst() {
            val processor = BatchProcessor()
            val txLow  = tx("ACC0500", "ACC0501", 10.0, priority = 1, timestamp = BASE_TS)
            val txMid  = tx("ACC0502", "ACC0503", 10.0, priority = 3, timestamp = BASE_TS)
            val txHigh = tx("ACC0504", "ACC0505", 10.0, priority = 5, timestamp = BASE_TS)
            val result = processor.process(listOf(txLow, txMid, txHigh))
            assertEquals(3, result.processed)
        }

        @Test
        @DisplayName("preserves order among transactions with equal priority")
        fun preservesEqualPriorityOrder() {
            val processor = BatchProcessor()
            val transactions = (0 until 5).map { i ->
                tx(
                    "ACC${600 + i * 2}",
                    "ACC${601 + i * 2}",
                    10.0,
                    priority = 3,
                    timestamp = BASE_TS + i * 10_000L
                )
            }
            val result = processor.process(transactions)
            assertEquals(5, result.processed)
        }
    }

    // =========================================================================
    // 5. Category report (5 tests)
    // =========================================================================

    @Nested
    @DisplayName("Category report")
    inner class CategoryReportTests {

        @Test
        @DisplayName("reports correct totals for each category")
        fun correctTotals() {
            val processor = BatchProcessor()
            val transactions = listOf(
                tx("ACC0700", "ACC0701", 100.0, category = "PAYMENT", timestamp = BASE_TS),
                tx("ACC0702", "ACC0703", 200.0, category = "PAYMENT", timestamp = BASE_TS + 10_000),
                tx("ACC0704", "ACC0705", 300.0, category = "WITHDRAWAL", timestamp = BASE_TS + 20_000)
            )
            val result = processor.process(transactions)
            val payment = result.report.find { it.category == "PAYMENT" }
            val withdrawal = result.report.find { it.category == "WITHDRAWAL" }
            assertNotNull(payment)
            assertEquals(2, payment!!.count)
            assertEquals(300.0, payment.total, 1e-9)
            assertEquals(150.0, payment.average, 1e-9)
            assertNotNull(withdrawal)
            assertEquals(1, withdrawal!!.count)
            assertEquals(300.0, withdrawal.total, 1e-9)
        }

        @Test
        @DisplayName("reports correct average for a single category")
        fun correctAverage() {
            val processor = BatchProcessor()
            val transactions = listOf(
                tx("ACC0800", "ACC0801", 50.0, category = "FEE", timestamp = BASE_TS),
                tx("ACC0802", "ACC0803", 150.0, category = "FEE", timestamp = BASE_TS + 10_000),
                tx("ACC0804", "ACC0805", 100.0, category = "FEE", timestamp = BASE_TS + 20_000)
            )
            val result = processor.process(transactions)
            val fee = result.report.find { it.category == "FEE" }
            assertNotNull(fee)
            assertEquals(3, fee!!.count)
            assertEquals(300.0, fee.total, 1e-9)
            assertEquals(100.0, fee.average, 1e-9)
        }

        @Test
        @DisplayName("report contains only categories present in processed transactions")
        fun onlyPresentCategories() {
            val processor = BatchProcessor()
            val transactions = listOf(
                tx("ACC0900", "ACC0901", 10.0, category = "DEPOSIT")
            )
            val result = processor.process(transactions)
            assertEquals(1, result.report.size)
            assertEquals("DEPOSIT", result.report[0].category)
        }

        @Test
        @DisplayName("returns empty report for empty input")
        fun emptyInput() {
            val processor = BatchProcessor()
            val result = processor.process(emptyList())
            assertTrue(result.report.isEmpty())
        }

        @Test
        @DisplayName("returns empty report when all transactions are rejected")
        fun allRejected() {
            val processor = BatchProcessor()
            val overdraft = tx("ACC0950", "ACC0951", 999_999.0)
            val result = processor.process(listOf(overdraft))
            assertEquals(0, result.processed)
            assertTrue(result.report.isEmpty())
        }
    }

    // =========================================================================
    // 6. Integrated correctness — 100 records (3 tests)
    // =========================================================================

    @Nested
    @DisplayName("Integrated correctness (100 records)")
    inner class IntegrationTests {

        private fun build100Dataset(): List<Transaction> {
            val tmpFile = Files.createTempFile("batch_test_100_", ".csv").toFile()
            try {
                DataGenerator.generate(100, tmpFile)
                return tmpFile.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("id,") }
                    .map { Transaction.fromCsvLine(it) }
            } finally {
                tmpFile.delete()
            }
        }

        @Test
        @DisplayName("processed + duplicates + rejected = total input")
        fun countsAddUp() {
            val data = build100Dataset()
            val processor = BatchProcessor()
            val result = processor.process(data)
            assertEquals(data.size, result.processed + result.duplicates + result.rejected)
        }

        @Test
        @DisplayName("report category counts sum to processed count")
        fun reportTotalsAddUp() {
            val data = build100Dataset()
            val processor = BatchProcessor()
            val result = processor.process(data)
            val reportTotal = result.report.sumOf { it.count }
            assertEquals(result.processed, reportTotal)
        }

        @Test
        @DisplayName("is deterministic — same input yields same output")
        fun deterministic() {
            val data = build100Dataset()
            val r1 = BatchProcessor().process(data)
            val r2 = BatchProcessor().process(data)
            assertEquals(r1.processed, r2.processed)
            assertEquals(r1.duplicates, r2.duplicates)
            assertEquals(r1.rejected, r2.rejected)
            assertEquals(r1.fraudRings, r2.fraudRings)
        }
    }
}

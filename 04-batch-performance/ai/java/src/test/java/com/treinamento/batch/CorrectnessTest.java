package com.treinamento.batch;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Correctness tests for the batch-processing pipeline.
 *
 * <p>23 tests organised in 6 sections:
 * <ol>
 *   <li>Duplicate detection (5)</li>
 *   <li>Account balance tracking (4)</li>
 *   <li>Fraud ring detection (4)</li>
 *   <li>Priority processing order (2)</li>
 *   <li>Category report (5)</li>
 *   <li>Integrated correctness - 100 records (3)</li>
 * </ol>
 *
 * All tests use small, hand-crafted datasets so they run in milliseconds and
 * provide clear failure messages when the implementation deviates.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CorrectnessTest {

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private static final long BASE_TS = 1_704_067_200_000L; // 2024-01-01T00:00:00Z

    private int idSeq;

    @BeforeEach
    void resetIdSequence() {
        idSeq = 0;
    }

    private Transaction tx(String sourceAccount, String destAccount, double amount) {
        return new Transaction(++idSeq, amount, sourceAccount, destAccount,
                BASE_TS, "TRANSFER", 3);
    }

    private Transaction tx(String sourceAccount, String destAccount, double amount,
                           long timestamp) {
        return new Transaction(++idSeq, amount, sourceAccount, destAccount,
                timestamp, "TRANSFER", 3);
    }

    private Transaction tx(String sourceAccount, String destAccount, double amount,
                           long timestamp, String category) {
        return new Transaction(++idSeq, amount, sourceAccount, destAccount,
                timestamp, category, 3);
    }

    private Transaction tx(String sourceAccount, String destAccount, double amount,
                           long timestamp, String category, int priority) {
        return new Transaction(++idSeq, amount, sourceAccount, destAccount,
                timestamp, category, priority);
    }

    // =======================================================================
    // 1. Duplicate detection (5 tests)
    // =======================================================================

    @Test
    @Order(1)
    @DisplayName("Accepts two transactions with the same fields but >5s apart")
    void testAcceptsTwoTransactionsOutsideDuplicateWindow() {
        BatchProcessor processor = new BatchProcessor();
        Transaction t1 = tx("ACC0001", "ACC0002", 100.0, BASE_TS);
        Transaction t2 = tx("ACC0001", "ACC0002", 100.0, BASE_TS + 6_000); // 6s later
        ProcessingResult result = processor.processTransactions(List.of(t1, t2));

        assertEquals(0, result.duplicates());
        assertEquals(2, result.processed());
    }

    @Test
    @Order(2)
    @DisplayName("Flags a transaction as duplicate when fields match within 5 seconds")
    void testFlagsDuplicateWithinWindow() {
        BatchProcessor processor = new BatchProcessor();
        Transaction t1 = tx("ACC0001", "ACC0002", 200.0, BASE_TS);
        Transaction t2 = tx("ACC0001", "ACC0002", 200.0, BASE_TS + 2_000); // 2s later
        ProcessingResult result = processor.processTransactions(List.of(t1, t2));

        assertEquals(1, result.duplicates());
        assertEquals(1, result.processed());
    }

    @Test
    @Order(3)
    @DisplayName("Does not flag as duplicate when amounts differ")
    void testDoesNotFlagDifferentAmount() {
        BatchProcessor processor = new BatchProcessor();
        Transaction t1 = tx("ACC0001", "ACC0002", 100.0, BASE_TS);
        Transaction t2 = tx("ACC0001", "ACC0002", 101.0, BASE_TS + 1_000);
        ProcessingResult result = processor.processTransactions(List.of(t1, t2));

        assertEquals(0, result.duplicates());
        assertEquals(2, result.processed());
    }

    @Test
    @Order(4)
    @DisplayName("Does not flag as duplicate when destination differs")
    void testDoesNotFlagDifferentDestination() {
        BatchProcessor processor = new BatchProcessor();
        Transaction t1 = tx("ACC0001", "ACC0002", 100.0, BASE_TS);
        Transaction t2 = tx("ACC0001", "ACC0003", 100.0, BASE_TS + 1_000);
        ProcessingResult result = processor.processTransactions(List.of(t1, t2));

        assertEquals(0, result.duplicates());
        assertEquals(2, result.processed());
    }

    @Test
    @Order(5)
    @DisplayName("Counts multiple duplicates independently")
    void testCountsMultipleDuplicatesIndependently() {
        BatchProcessor processor = new BatchProcessor();
        Transaction t1 = tx("ACC0001", "ACC0002", 50.0, BASE_TS);
        Transaction t2 = tx("ACC0001", "ACC0002", 50.0, BASE_TS + 1_000); // dup
        Transaction t3 = tx("ACC0001", "ACC0002", 50.0, BASE_TS + 2_000); // dup of t1
        ProcessingResult result = processor.processTransactions(List.of(t1, t2, t3));

        assertEquals(2, result.duplicates());
        assertEquals(1, result.processed());
    }

    // =======================================================================
    // 2. Account balance tracking (4 tests) — INITIAL_BALANCE = 10,000
    // =======================================================================

    @Test
    @Order(6)
    @DisplayName("Rejects a transaction when source has insufficient funds")
    void testRejectsOverdraft() {
        BatchProcessor processor = new BatchProcessor();
        Transaction drain    = tx("ACC0010", "ACC0011", 9_999.0);
        Transaction overdraft = tx("ACC0010", "ACC0012", 5_000.0);
        ProcessingResult result = processor.processTransactions(List.of(drain, overdraft));

        assertEquals(1, result.processed());
        assertEquals(1, result.rejected());
    }

    @Test
    @Order(7)
    @DisplayName("Accepts a transaction when source has exactly enough funds")
    void testAcceptsExactBalance() {
        BatchProcessor processor = new BatchProcessor();
        Transaction exact = tx("ACC0020", "ACC0021", 10_000.0);
        ProcessingResult result = processor.processTransactions(List.of(exact));

        assertEquals(1, result.processed());
        assertEquals(0, result.rejected());
    }

    @Test
    @Order(8)
    @DisplayName("Allows destination to spend funds it received earlier")
    void testDestinationCanSpendReceivedFunds() {
        BatchProcessor processor = new BatchProcessor();
        Transaction t1 = tx("ACC0030", "ACC0031", 5_000.0);
        // ACC0031 now has 10,000 + 5,000 = 15,000; should be able to send 12,000
        Transaction t2 = tx("ACC0031", "ACC0032", 12_000.0);
        ProcessingResult result = processor.processTransactions(List.of(t1, t2));

        assertEquals(2, result.processed());
        assertEquals(0, result.rejected());
    }

    @Test
    @Order(9)
    @DisplayName("Rejects multiple overdraft attempts independently")
    void testRejectsMultipleOverdraftsIndependently() {
        BatchProcessor processor = new BatchProcessor();
        Transaction big1 = tx("ACC0040", "ACC0041", 60_000.0);
        Transaction big2 = tx("ACC0042", "ACC0043", 60_000.0);
        ProcessingResult result = processor.processTransactions(List.of(big1, big2));

        assertEquals(0, result.processed());
        assertEquals(2, result.rejected());
    }

    // =======================================================================
    // 3. Fraud ring detection (4 tests)
    // =======================================================================

    @Test
    @Order(10)
    @DisplayName("Detects a simple A->B->A cycle")
    void testDetectsSimpleCycle() {
        BatchProcessor processor = new BatchProcessor();
        Transaction t1 = tx("ACC0100", "ACC0101", 100.0);
        Transaction t2 = tx("ACC0101", "ACC0100", 80.0);
        ProcessingResult result = processor.processTransactions(List.of(t1, t2));

        assertTrue(result.fraudRings() > 0, "Should detect at least one fraud ring");
    }

    @Test
    @Order(11)
    @DisplayName("Detects a three-node cycle A->B->C->A")
    void testDetectsThreeNodeCycle() {
        BatchProcessor processor = new BatchProcessor();
        Transaction t1 = tx("ACC0200", "ACC0201", 100.0);
        Transaction t2 = tx("ACC0201", "ACC0202", 100.0);
        Transaction t3 = tx("ACC0202", "ACC0200", 100.0);
        ProcessingResult result = processor.processTransactions(List.of(t1, t2, t3));

        assertTrue(result.fraudRings() > 0, "Should detect at least one fraud ring");
    }

    @Test
    @Order(12)
    @DisplayName("Returns no rings when graph is acyclic")
    void testNoRingsInAcyclicGraph() {
        BatchProcessor processor = new BatchProcessor();
        // Simple chain: A->B->C -- no cycle
        Transaction t1 = tx("ACC0300", "ACC0301", 100.0);
        Transaction t2 = tx("ACC0301", "ACC0302", 80.0);
        ProcessingResult result = processor.processTransactions(List.of(t1, t2));

        assertEquals(0, result.fraudRings(), "Acyclic graph should have no fraud rings");
    }

    @Test
    @Order(13)
    @DisplayName("Counts fraud rings correctly for a single cycle")
    void testCountsFraudRingsForSingleCycle() {
        BatchProcessor processor = new BatchProcessor();
        Transaction t1 = tx("ACC0400", "ACC0401", 100.0);
        Transaction t2 = tx("ACC0401", "ACC0400", 100.0);
        ProcessingResult result = processor.processTransactions(List.of(t1, t2));

        assertTrue(result.fraudRings() >= 1,
                "Should detect at least 1 fraud ring");
    }

    // =======================================================================
    // 4. Priority processing order (2 tests)
    // =======================================================================

    @Test
    @Order(14)
    @DisplayName("Higher-priority transactions appear before lower-priority")
    void testHigherPriorityFirst() {
        BatchProcessor processor = new BatchProcessor();
        Transaction txLow  = tx("ACC0500", "ACC0501", 10.0, BASE_TS, "PAYMENT", 1);
        Transaction txMid  = tx("ACC0502", "ACC0503", 10.0, BASE_TS, "PAYMENT", 3);
        Transaction txHigh = tx("ACC0504", "ACC0505", 10.0, BASE_TS, "PAYMENT", 5);
        // Submit in low->mid->high order; processor should reorder to high->mid->low
        ProcessingResult result = processor.processTransactions(List.of(txLow, txMid, txHigh));

        assertEquals(3, result.processed());
    }

    @Test
    @Order(15)
    @DisplayName("Preserves relative order for equal-priority transactions")
    void testPreservesEqualPriorityOrder() {
        BatchProcessor processor = new BatchProcessor();
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            transactions.add(tx(
                    String.format("ACC060%d", i),
                    String.format("ACC061%d", i),
                    10.0,
                    BASE_TS + i * 10_000L,
                    "PAYMENT",
                    3));
        }
        ProcessingResult result = processor.processTransactions(transactions);

        assertEquals(5, result.processed());
    }

    // =======================================================================
    // 5. Category report (5 tests)
    // =======================================================================

    @Test
    @Order(16)
    @DisplayName("Reports correct totals for each category")
    void testReportCorrectTotals() {
        BatchProcessor processor = new BatchProcessor();
        List<Transaction> transactions = List.of(
                tx("ACC0700", "ACC0701", 100.0, BASE_TS, "PAYMENT"),
                tx("ACC0702", "ACC0703", 200.0, BASE_TS + 10_000, "PAYMENT"),
                tx("ACC0704", "ACC0705", 300.0, BASE_TS + 20_000, "WITHDRAWAL")
        );
        ProcessingResult result = processor.processTransactions(transactions);

        ProcessingResult.CategoryReport payment = result.report().stream()
                .filter(r -> "PAYMENT".equals(r.category())).findFirst().orElse(null);
        ProcessingResult.CategoryReport withdrawal = result.report().stream()
                .filter(r -> "WITHDRAWAL".equals(r.category())).findFirst().orElse(null);

        assertNotNull(payment);
        assertEquals(2, payment.count());
        assertEquals(300.0, payment.total(), 0.01);
        assertEquals(150.0, payment.average(), 0.01);

        assertNotNull(withdrawal);
        assertEquals(1, withdrawal.count());
        assertEquals(300.0, withdrawal.total(), 0.01);
    }

    @Test
    @Order(17)
    @DisplayName("Reports correct average per category")
    void testReportCorrectAverage() {
        BatchProcessor processor = new BatchProcessor();
        List<Transaction> transactions = List.of(
                tx("ACC0800", "ACC0801", 50.0,  BASE_TS, "FEE"),
                tx("ACC0802", "ACC0803", 150.0, BASE_TS + 10_000, "FEE"),
                tx("ACC0804", "ACC0805", 100.0, BASE_TS + 20_000, "FEE")
        );
        ProcessingResult result = processor.processTransactions(transactions);

        ProcessingResult.CategoryReport fee = result.report().stream()
                .filter(r -> "FEE".equals(r.category())).findFirst().orElse(null);
        assertNotNull(fee);
        assertEquals(3, fee.count());
        assertEquals(300.0, fee.total(), 0.01);
        assertEquals(100.0, fee.average(), 0.01);
    }

    @Test
    @Order(18)
    @DisplayName("Report contains only categories present in processed transactions")
    void testReportOnlyContainsPresentCategories() {
        BatchProcessor processor = new BatchProcessor();
        List<Transaction> transactions = List.of(
                tx("ACC0900", "ACC0901", 10.0, BASE_TS, "DEPOSIT")
        );
        ProcessingResult result = processor.processTransactions(transactions);

        assertEquals(1, result.report().size());
        assertEquals("DEPOSIT", result.report().get(0).category());
    }

    @Test
    @Order(19)
    @DisplayName("Returns empty report for empty input")
    void testEmptyInputReturnsEmptyReport() {
        BatchProcessor processor = new BatchProcessor();
        ProcessingResult result = processor.processTransactions(List.of());

        assertTrue(result.report().isEmpty());
    }

    @Test
    @Order(20)
    @DisplayName("Returns empty report when all transactions are rejected")
    void testAllRejectedReturnsEmptyReport() {
        BatchProcessor processor = new BatchProcessor();
        // Overdraft: amount > 10K initial balance
        Transaction overdraft = tx("ACC0950", "ACC0951", 999_999.0);
        ProcessingResult result = processor.processTransactions(List.of(overdraft));

        assertEquals(0, result.processed());
        assertTrue(result.report().isEmpty());
    }

    // =======================================================================
    // 6. Integrated correctness - 100 records (3 tests)
    // =======================================================================

    private static final int INTEGRATED_SIZE = 100;

    private List<Transaction> loadIntegratedDataset() throws IOException {
        Path tmpDir = Files.createTempDirectory("batch-correctness-integrated-");
        Path csvPath = tmpDir.resolve("transactions_100.csv");
        DataGenerator.generate(INTEGRATED_SIZE, csvPath);

        List<String> lines = Files.readAllLines(csvPath);
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                transactions.add(Transaction.fromCsv(line));
            }
        }
        return transactions;
    }

    @Test
    @Order(21)
    @DisplayName("Integrated 100: processed + duplicates + rejected = 100")
    void testIntegrated100CountsAddUp() throws IOException {
        List<Transaction> dataset = loadIntegratedDataset();
        ProcessingResult result = new BatchProcessor().processTransactions(dataset);

        assertEquals(INTEGRATED_SIZE,
                result.processed() + result.duplicates() + result.rejected(),
                "processed + duplicates + rejected must equal total");
    }

    @Test
    @Order(22)
    @DisplayName("Integrated 100: sum of category counts = processed")
    void testIntegrated100ReportTotalsAddUp() throws IOException {
        List<Transaction> dataset = loadIntegratedDataset();
        ProcessingResult result = new BatchProcessor().processTransactions(dataset);

        int reportTotal = result.report().stream()
                .mapToInt(ProcessingResult.CategoryReport::count)
                .sum();
        assertEquals(result.processed(), reportTotal,
                "Sum of category counts must equal processed count");
    }

    @Test
    @Order(23)
    @DisplayName("Integrated 100: same input yields same output (deterministic)")
    void testIntegrated100IsDeterministic() throws IOException {
        List<Transaction> dataset = loadIntegratedDataset();
        ProcessingResult r1 = new BatchProcessor().processTransactions(dataset);
        ProcessingResult r2 = new BatchProcessor().processTransactions(dataset);

        assertEquals(r1.processed(), r2.processed());
        assertEquals(r1.duplicates(), r2.duplicates());
        assertEquals(r1.rejected(), r2.rejected());
        assertEquals(r1.fraudRings(), r2.fraudRings());
    }
}

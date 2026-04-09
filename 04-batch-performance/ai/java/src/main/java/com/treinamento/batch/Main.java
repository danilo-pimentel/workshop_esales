package com.treinamento.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CLI entry point for the batch transaction processor.
 *
 * <h2>Usage</h2>
 * <pre>
 *   mvn exec:java -Dexec.args="data/transactions_10k.csv"
 *   mvn exec:java -Dexec.args="data/transactions_10k.csv --generate 10000"
 * </pre>
 */
public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: batch-processor <transactions.csv> [--generate <count>]");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  mvn exec:java -Dexec.args=\"data/transactions_10k.csv\"");
            System.err.println("  mvn exec:java -Dexec.args=\"data/transactions_10k.csv --generate 10000\"");
            System.exit(1);
        }

        Path csvPath = Path.of(args[0]);

        // Optional: auto-generate the file if it is missing and --generate is supplied
        if (!Files.exists(csvPath)) {
            if (args.length >= 3 && "--generate".equals(args[1])) {
                int count = Integer.parseInt(args[2]);
                autoGenerate(csvPath, count);
            } else {
                System.err.println("File not found: " + csvPath);
                System.err.println("Hint: add --generate <count> to auto-generate it, e.g.:");
                System.err.println("  mvn exec:java -Dexec.args=\"" + args[0]
                        + " --generate 10000\"");
                System.exit(1);
            }
        }

        // Read transactions
        System.out.println("Reading transactions from: " + csvPath.toAbsolutePath());
        List<Transaction> transactions;
        try {
            List<String> lines = Files.readAllLines(csvPath);
            transactions = new java.util.ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("id,")) continue;
                try {
                    transactions.add(Transaction.fromCsv(trimmed));
                } catch (Exception e) {
                    // skip malformed rows
                }
            }
        } catch (IOException e) {
            System.err.println("I/O error reading " + csvPath + ": " + e.getMessage());
            System.exit(2);
            return;
        }
        System.out.printf("Loaded %,d transactions.%n%n", transactions.size());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("Inicio: " + LocalDateTime.now().format(dtf));
        System.out.println();

        BatchProcessor processor = new BatchProcessor();
        long startTime = System.currentTimeMillis();
        ProcessingResult result = processor.processTransactions(transactions);
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;

        System.out.println();
        System.out.println("Termino: " + LocalDateTime.now().format(dtf));
        System.out.println();

        // -------------------------------------------------------------------------
        // Summary output — matches Bun format exactly
        // -------------------------------------------------------------------------
        System.out.println("=".repeat(60));
        System.out.println("  BATCH PROCESSING RESULTS");
        System.out.println("=".repeat(60));
        System.out.printf(java.util.Locale.US, "  Processed   : %,d%n", result.processed());
        System.out.printf(java.util.Locale.US, "  Duplicates  : %,d%n", result.duplicates());
        System.out.printf(java.util.Locale.US, "  Rejected    : %,d%n", result.rejected());
        System.out.printf(java.util.Locale.US, "  Fraud rings : %,d%n", result.fraudRings());
        System.out.printf(java.util.Locale.US, "  Time        : %.3fs%n", elapsed);
        System.out.println("=".repeat(60));

        // Category Report
        System.out.println();
        System.out.println("Category Report:");
        System.out.printf("  %-16s  %8s  %14s  %12s%n",
            "Category", "Count", "Total", "Average");
        System.out.println("  " + "-".repeat(54));
        for (ProcessingResult.CategoryReport stat : result.report()) {
            System.out.printf(java.util.Locale.US, "  %-16s  %8d  %14.2f  %12.2f%n",
                stat.category(), stat.count(), stat.total(), stat.average());
        }

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static void autoGenerate(Path csvPath, int count) {
        try {
            if (csvPath.getParent() != null) {
                Files.createDirectories(csvPath.getParent());
            }
            System.out.printf("Generating %,d transactions → %s%n", count, csvPath);
            DataGenerator.generate(count, csvPath);
            System.out.println("Generation complete.");
            System.out.println();
        } catch (IOException e) {
            System.err.println("Failed to generate dataset: " + e.getMessage());
            System.exit(1);
        }
    }
}

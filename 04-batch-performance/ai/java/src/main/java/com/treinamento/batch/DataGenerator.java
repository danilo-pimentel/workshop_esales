package com.treinamento.batch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates synthetic CSV transaction datasets for the batch-performance
 * exercise.
 *
 * <p>Uses mulberry32 PRNG with seed=42 so that ALL languages (Java, Kotlin,
 * PHP, Bun) produce bit-identical CSV output.
 */
public class DataGenerator {

    private static final int ACCOUNTS_COUNT = 1_000;
    private static final String[] CATEGORIES = {
        "PAYMENT", "TRANSFER", "REFUND", "PURCHASE",
        "WITHDRAWAL", "DEPOSIT", "FEE", "INTEREST"
    };

    public static final String CSV_HEADER =
        "id,amount,source_account,dest_account,timestamp,category,priority";

    // -------------------------------------------------------------------------
    // mulberry32 PRNG — MUST be identical across all 4 languages
    // -------------------------------------------------------------------------

    private int prngState;

    private DataGenerator(int seed) {
        this.prngState = seed;
    }

    private double nextRandom() {
        prngState += 0x6d2b79f5;
        int t = (prngState ^ (prngState >>> 15)) * (1 | prngState);
        t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
        return Integer.toUnsignedLong(t ^ (t >>> 14)) / 4294967296.0;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String padAccount(int n) {
        return String.format("ACC%04d", n);
    }

    private String randomAccount() {
        return padAccount((int) (nextRandom() * ACCOUNTS_COUNT) + 1);
    }

    private String randomAmount() {
        double amount = 10 + nextRandom() * 9990;
        return String.format(Locale.US, "%.2f", amount);
    }

    private long randomTimestamp(long baseMs) {
        long offsetMs = (long) (nextRandom() * 366.0 * 24 * 60 * 60 * 1000);
        return baseMs + offsetMs;
    }

    private String randomCategory() {
        return CATEGORIES[(int) (nextRandom() * CATEGORIES.length)];
    }

    private int randomPriority() {
        return (int) (nextRandom() * 5) + 1;
    }

    // -------------------------------------------------------------------------
    // Pool entry for duplicate generation
    // -------------------------------------------------------------------------

    private static class PoolEntry {
        final String amount;
        final String source;
        final String dest;
        final long timestamp;
        final String category;
        final int priority;

        PoolEntry(String amount, String source, String dest, long timestamp,
                  String category, int priority) {
            this.amount = amount;
            this.source = source;
            this.dest = dest;
            this.timestamp = timestamp;
            this.category = category;
            this.priority = priority;
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Generates {@code count} transaction records and writes them (with header)
     * to {@code outputPath}. Uses fixed seed=42 for cross-language reproducibility.
     */
    public static void generate(int count, Path outputPath) throws IOException {
        DataGenerator gen = new DataGenerator(42);  // Fixed seed
        long base2024 = 1_704_067_200_000L;  // 2024-01-01T00:00:00Z

        // Pool for duplicates (matches Bun's recentPool — ArrayList with shift)
        List<PoolEntry> recentPool = new ArrayList<>();
        int poolSize = 50;

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write(CSV_HEADER + "\n");

            for (int i = 1; i <= count; i++) {
                boolean isDuplicate = gen.nextRandom() < 0.02 && !recentPool.isEmpty();
                boolean isOverdraft = gen.nextRandom() < 0.01;

                String amount, source, dest, category;
                long ts;
                int priority;

                if (isDuplicate) {
                    int poolIndex = (int) (gen.nextRandom() * recentPool.size());
                    PoolEntry original = recentPool.get(poolIndex);
                    int shiftMs = (int) (gen.nextRandom() * 2_000);
                    ts = original.timestamp + shiftMs;
                    amount = original.amount;
                    source = original.source;
                    dest = original.dest;
                    category = original.category;
                    priority = original.priority;
                } else if (isOverdraft) {
                    amount = "60000.00";
                    source = gen.randomAccount();
                    dest = gen.randomAccount();
                    ts = gen.randomTimestamp(base2024);
                    category = gen.randomCategory();
                    priority = gen.randomPriority();
                } else {
                    amount = gen.randomAmount();
                    source = gen.randomAccount();
                    dest = gen.randomAccount();
                    while (dest.equals(source)) dest = gen.randomAccount();
                    ts = gen.randomTimestamp(base2024);
                    category = gen.randomCategory();
                    priority = gen.randomPriority();
                }

                writer.write(i + "," + amount + "," + source + "," + dest + ","
                    + ts + "," + category + "," + priority + "\n");

                // Maintain the recent pool for duplicate generation
                if (!isDuplicate) {
                    if (recentPool.size() >= poolSize) {
                        recentPool.remove(0);  // shift — remove oldest
                    }
                    recentPool.add(new PoolEntry(amount, source, dest, ts,
                        category, priority));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Main — standalone generator
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: DataGenerator <count> <outputFile>");
            System.exit(1);
        }
        int count = Integer.parseInt(args[0]);
        Path output = Path.of(args[1]);
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        System.out.printf("Generating %,d transactions → %s%n", count, output);
        long t0 = System.currentTimeMillis();
        generate(count, output);
        System.out.printf("Done in %d ms%n", System.currentTimeMillis() - t0);
    }
}

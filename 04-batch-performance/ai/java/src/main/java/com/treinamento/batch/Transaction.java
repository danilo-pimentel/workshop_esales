package com.treinamento.batch;

/**
 * Immutable data model representing a single financial transaction.
 *
 * CSV columns (in order):
 *   id, amount, source_account, dest_account, timestamp, category, priority
 */
public record Transaction(
    int id,
    double amount,
    String sourceAccount,
    String destAccount,
    long timestamp,       // epoch millis
    String category,
    int priority          // 1 (lowest) .. 5 (highest)
) {

    /**
     * Parses a CSV line produced by DataGenerator.
     * Expected format: id,amount,source_account,dest_account,timestamp,category,priority
     */
    public static Transaction fromCsv(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != 7) {
            throw new IllegalArgumentException(
                "Invalid CSV line (expected 7 columns): " + line);
        }
        return new Transaction(
            Integer.parseInt(parts[0].trim()),
            Double.parseDouble(parts[1].trim()),
            parts[2].trim(),
            parts[3].trim(),
            Long.parseLong(parts[4].trim()),
            parts[5].trim(),
            Integer.parseInt(parts[6].trim())
        );
    }

    /** CSV representation (mirrors fromCsv). */
    public String toCsv() {
        return String.join(",",
            String.valueOf(id), String.valueOf(amount),
            sourceAccount, destAccount,
            String.valueOf(timestamp), category, String.valueOf(priority));
    }

    /**
     * Two transactions are considered potential duplicates when they share the
     * same source account, destination account, and amount. The time-window
     * check (within 5 seconds) is done in BatchProcessor.
     */
    public boolean isSameOperation(Transaction other) {
        return this.sourceAccount.equals(other.sourceAccount)
            && this.destAccount.equals(other.destAccount)
            && Double.compare(this.amount, other.amount) == 0;
    }
}

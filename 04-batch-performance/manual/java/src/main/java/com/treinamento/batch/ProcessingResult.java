package com.treinamento.batch;

import java.util.List;

/**
 * Immutable summary produced by {@link BatchProcessor} after processing a batch.
 */
public record ProcessingResult(
    int processed,
    int duplicates,
    int rejected,
    int fraudRings,
    List<CategoryReport> report
) {

    /**
     * Per-category aggregation.
     */
    public record CategoryReport(
        String category,
        int count,
        double total,
        double average
    ) {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(60)).append('\n');
        sb.append("  BATCH PROCESSING RESULTS\n");
        sb.append("=".repeat(60)).append('\n');
        sb.append(String.format("  Processed   : %,d%n", processed));
        sb.append(String.format("  Duplicates  : %,d%n", duplicates));
        sb.append(String.format("  Rejected    : %,d%n", rejected));
        sb.append(String.format("  Fraud rings : %,d%n", fraudRings));
        sb.append("=".repeat(60));
        return sb.toString();
    }
}

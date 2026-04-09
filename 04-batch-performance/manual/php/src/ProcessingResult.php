<?php

declare(strict_types=1);

namespace BatchProcessor;

/**
 * Aggregated result produced by BatchProcessor after processing a batch.
 */
class ProcessingResult
{
    public readonly int $processed;
    public readonly int $duplicates;
    public readonly int $rejected;
    public readonly int $fraudRings;
    /** @var CategoryStats[] */
    public readonly array $report;

    /**
     * @param CategoryStats[] $report
     */
    public function __construct(
        int   $processed,
        int   $duplicates,
        int   $rejected,
        int   $fraudRings,
        array $report,
    ) {
        $this->processed = $processed;
        $this->duplicates = $duplicates;
        $this->rejected = $rejected;
        $this->fraudRings = $fraudRings;
        $this->report = $report;
    }
}

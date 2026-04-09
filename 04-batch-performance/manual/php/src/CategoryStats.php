<?php

declare(strict_types=1);

namespace BatchProcessor;

/**
 * Stats for a single category.
 */
class CategoryStats
{
    public readonly string $category;
    public readonly int $count;
    public readonly float $total;
    public readonly float $average;

    public function __construct(string $category, int $count, float $total, float $average)
    {
        $this->category = $category;
        $this->count = $count;
        $this->total = $total;
        $this->average = $average;
    }
}

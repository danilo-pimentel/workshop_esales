<?php

declare(strict_types=1);

namespace BatchProcessor;

/**
 * Value object representing a single financial transaction.
 */
class Transaction
{
    public readonly int $id;
    public readonly float $amount;
    public readonly string $sourceAccount;
    public readonly string $destAccount;
    public readonly int $timestamp;  // epoch ms (PHP int is 64-bit)
    public readonly string $category;
    public readonly int $priority;

    public function __construct(
        int    $id,
        float  $amount,
        string $sourceAccount,
        string $destAccount,
        int    $timestamp,
        string $category,
        int    $priority,
    ) {
        $this->id = $id;
        $this->amount = $amount;
        $this->sourceAccount = $sourceAccount;
        $this->destAccount = $destAccount;
        $this->timestamp = $timestamp;
        $this->category = $category;
        $this->priority = $priority;
    }

    /**
     * Create a Transaction from a CSV line string.
     * Expected columns: id,amount,source_account,dest_account,timestamp,category,priority
     */
    public static function fromCsvRow(string $line): self
    {
        $parts = explode(',', $line);
        if (count($parts) !== 7) {
            throw new \InvalidArgumentException("Invalid CSV line: $line");
        }
        return new self(
            (int)trim($parts[0]),
            (float)trim($parts[1]),
            trim($parts[2]),
            trim($parts[3]),
            (int)trim($parts[4]),
            trim($parts[5]),
            (int)trim($parts[6]),
        );
    }
}

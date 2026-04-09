<?php

declare(strict_types=1);

/**
 * DataGenerator -- CSV transaction generator using mulberry32 PRNG.
 *
 * IMPORTANT: This generator uses mulberry32 PRNG with seed=42 so that
 * ALL languages (Java, Kotlin, PHP, Bun) produce bit-identical CSV output.
 *
 * Usage:
 *   php src/DataGenerator.php <count> <output-file>
 *   php src/DataGenerator.php 10000 data/transactions_10k.csv
 */

// ---------------------------------------------------------------------------
// mulberry32 -- Seedable 32-bit PRNG. MUST be identical across all 4 languages.
// ---------------------------------------------------------------------------

class Mulberry32
{
    private int $state;

    public function __construct(int $seed)
    {
        $this->state = $seed;
    }

    public function nextRandom(): float
    {
        $this->state = $this->to32($this->state + 0x6d2b79f5);
        $s = $this->state;

        $t = $this->imul($s ^ $this->ushr($s, 15), 1 | $s);
        $t = $this->to32(($t + $this->imul($t ^ $this->ushr($t, 7), 61 | $t)) ^ $t);

        return $this->ushr($t ^ $this->ushr($t, 14), 0) / 4294967296.0;
    }

    /** Convert to signed 32-bit integer (simulates JavaScript's |0) */
    private function to32(int $v): int
    {
        $v = $v & 0xFFFFFFFF;
        if ($v >= 0x80000000) {
            $v -= 0x100000000;
        }
        return (int)$v;
    }

    /** Unsigned right shift (JavaScript's >>>) */
    private function ushr(int $v, int $n): int
    {
        $v = $v & 0xFFFFFFFF; // to unsigned 32-bit
        if ($n === 0) {
            return $v;
        }
        return ($v >> $n) & 0xFFFFFFFF;
    }

    /** JavaScript's Math.imul -- 32-bit integer multiply */
    private function imul(int $a, int $b): int
    {
        $a = $this->to32($a);
        $b = $this->to32($b);
        // Multiply using low 16 bits to avoid PHP integer overflow
        $ah = ($a >> 16) & 0xFFFF;
        $al = $a & 0xFFFF;
        $bh = ($b >> 16) & 0xFFFF;
        $bl = $b & 0xFFFF;
        $result = (($al * $bl) + ((($ah * $bl + $al * $bh) & 0xFFFF) << 16));
        return $this->to32($result);
    }
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const ACCOUNTS_COUNT = 1000;
const CATEGORIES = [
    'PAYMENT',
    'TRANSFER',
    'REFUND',
    'PURCHASE',
    'WITHDRAWAL',
    'DEPOSIT',
    'FEE',
    'INTEREST',
];

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function padAccount(int $n): string
{
    return sprintf('ACC%04d', $n);
}

function randomAccount(Mulberry32 $rand): string
{
    return padAccount((int)floor($rand->nextRandom() * ACCOUNTS_COUNT) + 1);
}

function randomAmount(Mulberry32 $rand): string
{
    // Range: 10.00 - 10000.00
    $amount = 10 + $rand->nextRandom() * 9990;
    return sprintf('%.2f', $amount);
}

function randomTimestamp(Mulberry32 $rand, int $baseMs): int
{
    // Spread across the year 2024 (~ 366 days in milliseconds)
    $offsetMs = (int)floor($rand->nextRandom() * 366 * 24 * 60 * 60 * 1000);
    return $baseMs + $offsetMs;
}

function randomCategory(Mulberry32 $rand): string
{
    return CATEGORIES[(int)floor($rand->nextRandom() * count(CATEGORIES))];
}

function randomPriority(Mulberry32 $rand): int
{
    return (int)floor($rand->nextRandom() * 5) + 1;
}

// ---------------------------------------------------------------------------
// Standalone generation function (used by tests too)
// ---------------------------------------------------------------------------

function generateTransactionsCsv(int $n): string
{
    $rand = new Mulberry32(42); // fixed seed -- identical across all languages
    // 2024-01-01T00:00:00Z in epoch milliseconds
    $base2024 = 1704067200000;

    $lines = ["id,amount,source_account,dest_account,timestamp,category,priority"];

    // Keep a small pool of recent rows to create intentional duplicates from
    $recentPool = [];
    $POOL_SIZE = 50;

    for ($i = 1; $i <= $n; $i++) {
        $isDuplicate = $rand->nextRandom() < 0.02 && count($recentPool) > 0; // ~2% duplicates
        $isOverdraft = $rand->nextRandom() < 0.01; // ~1% that will overdraft

        if ($isDuplicate) {
            // Re-use a row from the recent pool (same amount/src/dst)
            $original = $recentPool[(int)floor($rand->nextRandom() * count($recentPool))];
            // Shift timestamp by <= 2 seconds to stay within the 5-second dup window
            $shiftMs = (int)floor($rand->nextRandom() * 2000); // 0-1999 ms
            $ts = $original['timestamp'] + $shiftMs;
            $amount = $original['amount'];
            $source = $original['source'];
            $dest = $original['dest'];
            $category = $original['category'];
            $priority = $original['priority'];
        } elseif ($isOverdraft) {
            // Use an unrealistically large amount to guarantee balance rejection
            $amount = '60000.00';
            $source = randomAccount($rand);
            $dest = randomAccount($rand);
            $ts = randomTimestamp($rand, $base2024);
            $category = randomCategory($rand);
            $priority = randomPriority($rand);
        } else {
            $amount = randomAmount($rand);
            $source = randomAccount($rand);
            $dest = randomAccount($rand);
            // Ensure source != dest
            while ($dest === $source) {
                $dest = randomAccount($rand);
            }
            $ts = randomTimestamp($rand, $base2024);
            $category = randomCategory($rand);
            $priority = randomPriority($rand);
        }

        $lines[] = "{$i},{$amount},{$source},{$dest},{$ts},{$category},{$priority}";

        // Maintain the recent pool for duplicate generation
        if (!$isDuplicate) {
            if (count($recentPool) >= $POOL_SIZE) {
                array_shift($recentPool);
            }
            $recentPool[] = [
                'amount' => $amount,
                'source' => $source,
                'dest' => $dest,
                'timestamp' => $ts,
                'category' => $category,
                'priority' => $priority,
            ];
        }
    }

    return implode("\n", $lines) . "\n";
}

// ---------------------------------------------------------------------------
// CLI entry point
// ---------------------------------------------------------------------------

if (php_sapi_name() === 'cli' && isset($argv[0]) && realpath($argv[0]) === realpath(__FILE__)) {
    $args = array_slice($argv, 1);
    $n = isset($args[0]) ? (int)$args[0] : 10000;
    $outputFile = $args[1] ?? null;

    if ($n <= 0) {
        fwrite(STDERR, "Usage: php src/DataGenerator.php <positive-integer> [output-file]\n");
        exit(1);
    }

    $content = generateTransactionsCsv($n);

    if ($outputFile !== null) {
        file_put_contents($outputFile, $content);
        fwrite(STDERR, "Generated " . number_format($n) . " transactions -> {$outputFile}\n");
    } else {
        echo $content;
    }
}

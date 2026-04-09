<?php

declare(strict_types=1);

/**
 * CLI entry-point for the batch processor.
 *
 * Usage:
 *   php src/index.php <csv-file>
 *   php src/index.php data/transactions_10k.csv
 */

// -- Bootstrap ----------------------------------------------------------------

$autoload = __DIR__ . '/../vendor/autoload.php';

if (!file_exists($autoload)) {
    fwrite(STDERR, "Error: vendor/autoload.php not found.\n");
    fwrite(STDERR, "Run:  composer install\n");
    exit(1);
}

require $autoload;

use BatchProcessor\BatchProcessor;
use BatchProcessor\Transaction;

// -- Argument validation ------------------------------------------------------

if ($argc < 2) {
    fwrite(STDERR, "Usage: php src/index.php <csv-file>\n");
    fwrite(STDERR, "Example: php src/index.php data/transactions_10k.csv\n");
    exit(1);
}

$csvPath = $argv[1];

if (!file_exists($csvPath)) {
    fwrite(STDERR, "Error: file not found: {$csvPath}\n");
    exit(1);
}

if (!is_readable($csvPath)) {
    fwrite(STDERR, "Error: file is not readable: {$csvPath}\n");
    exit(1);
}

// -- Load CSV -----------------------------------------------------------------

echo "Reading transactions from: {$csvPath}\n";

$csvText = file_get_contents($csvPath);
if ($csvText === false) {
    fwrite(STDERR, "Error: cannot read file: {$csvPath}\n");
    exit(1);
}

$lines = explode("\n", trim($csvText));
$transactions = [];

foreach ($lines as $line) {
    $trimmed = trim($line);
    if ($trimmed === '' || str_starts_with($trimmed, 'id,')) {
        continue; // skip header / blank
    }
    try {
        $transactions[] = Transaction::fromCsvRow($trimmed);
    } catch (\Throwable $e) {
        // skip malformed rows silently
    }
}

$loaded = count($transactions);
echo "Loaded " . number_format($loaded) . " transactions.\n\n";

if ($loaded === 0) {
    fwrite(STDERR, "Error: no valid transactions found in {$csvPath}\n");
    exit(1);
}

// -- Process ------------------------------------------------------------------

$fmtTime = fn() => date('Y-m-d H:i:s');

echo "Inicio: " . $fmtTime() . "\n\n";

$processor = new BatchProcessor();

$startTime = microtime(true);
$result = $processor->process($transactions);
$elapsed = sprintf('%.3f', microtime(true) - $startTime);

echo "\nTermino: " . $fmtTime() . "\n\n";

// -- Summary output -----------------------------------------------------------

echo str_repeat('=', 60) . "\n";
echo "  BATCH PROCESSING RESULTS\n";
echo str_repeat('=', 60) . "\n";
echo "  Processed   : " . number_format($result->processed) . "\n";
echo "  Duplicates  : " . number_format($result->duplicates) . "\n";
echo "  Rejected    : " . number_format($result->rejected) . "\n";
echo "  Fraud rings : " . number_format($result->fraudRings) . "\n";
echo "  Time        : {$elapsed}s\n";
echo str_repeat('=', 60) . "\n";

echo "\nCategory Report:\n";
echo "  "
    . str_pad('Category', 16) . "  "
    . str_pad('Count', 8, ' ', STR_PAD_LEFT) . "  "
    . str_pad('Total', 14, ' ', STR_PAD_LEFT) . "  "
    . str_pad('Average', 12, ' ', STR_PAD_LEFT)
    . "\n";
echo "  " . str_repeat('-', 54) . "\n";

foreach ($result->report as $stat) {
    echo "  "
        . str_pad($stat->category, 16) . "  "
        . str_pad((string)$stat->count, 8, ' ', STR_PAD_LEFT) . "  "
        . str_pad(sprintf('%.2f', $stat->total), 14, ' ', STR_PAD_LEFT) . "  "
        . str_pad(sprintf('%.2f', $stat->average), 12, ' ', STR_PAD_LEFT)
        . "\n";
}

echo "\n";

exit(0);

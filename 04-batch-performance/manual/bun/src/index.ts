// ---------------------------------------------------------------------------
// index.ts — CLI entry point
//
// Usage:
//   bun run src/index.ts <path-to-csv>
//
// Reads a CSV file of transactions, runs BatchProcessor, prints a summary.
// ---------------------------------------------------------------------------

import { readFileSync } from "fs";
import { parseCSV } from "./models";
import { BatchProcessor } from "./processor";

function main(): void {
  const args = process.argv.slice(2);
  if (args.length === 0) {
    console.error("Usage: bun run src/index.ts <csv-file>");
    console.error("Example: bun run src/index.ts data/transactions_10k.csv");
    process.exit(1);
  }

  const csvPath = args[0];
  let csvText: string;
  try {
    csvText = readFileSync(csvPath, "utf-8");
  } catch (err) {
    console.error(`Error reading file "${csvPath}":`, (err as Error).message);
    process.exit(1);
  }

  console.log(`Reading transactions from: ${csvPath}`);
  const transactions = parseCSV(csvText);
  console.log(`Loaded ${transactions.length.toLocaleString()} transactions.\n`);

  const fmtTime = () => new Date().toISOString().replace("T", " ").slice(0, 19);

  console.log(`Inicio: ${fmtTime()}\n`);

  const processor = new BatchProcessor();

  const startTime = performance.now();
  const result = processor.process(transactions);
  const elapsed = ((performance.now() - startTime) / 1000).toFixed(3);

  console.log(`\nTermino: ${fmtTime()}\n`);

  // -------------------------------------------------------------------------
  // Summary output
  // -------------------------------------------------------------------------
  console.log("=".repeat(60));
  console.log("  BATCH PROCESSING RESULTS");
  console.log("=".repeat(60));
  console.log(`  Processed   : ${result.processed.toLocaleString()}`);
  console.log(`  Duplicates  : ${result.duplicates.toLocaleString()}`);
  console.log(`  Rejected    : ${result.rejected.toLocaleString()}`);
  console.log(`  Fraud rings : ${result.fraudRings.toLocaleString()}`);
  console.log(`  Time        : ${elapsed}s`);
  console.log("=".repeat(60));

  console.log("\nCategory Report:");
  console.log(
    "  " +
      ["Category".padEnd(16), "Count".padStart(8), "Total".padStart(14), "Average".padStart(12)].join("  ")
  );
  console.log("  " + "-".repeat(54));
  for (const stat of result.report) {
    console.log(
      "  " +
        [
          stat.category.padEnd(16),
          stat.count.toString().padStart(8),
          stat.total.toFixed(2).padStart(14),
          stat.average.toFixed(2).padStart(12),
        ].join("  ")
    );
  }

  console.log();
}

main();

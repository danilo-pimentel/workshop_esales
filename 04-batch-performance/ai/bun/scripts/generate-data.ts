// ---------------------------------------------------------------------------
// generate-data.ts — Generates synthetic transaction CSV datasets.
//
// IMPORTANT: This generator uses mulberry32 PRNG with seed=42 so that
// ALL languages (Java, Kotlin, PHP, Bun) produce bit-identical CSV output.
//
// Usage:
//   bun run scripts/generate-data.ts <n> [output-file]
//
// Examples:
//   bun run scripts/generate-data.ts 1000   data/transactions_1k.csv
//   bun run scripts/generate-data.ts 10000  data/transactions_10k.csv
//   bun run scripts/generate-data.ts 1000000 data/transactions_1m.csv
// ---------------------------------------------------------------------------

import { writeFileSync } from "fs";

const ACCOUNTS_COUNT = 1_000;
const CATEGORIES = [
  "PAYMENT",
  "TRANSFER",
  "REFUND",
  "PURCHASE",
  "WITHDRAWAL",
  "DEPOSIT",
  "FEE",
  "INTEREST",
] as const;

// ---------------------------------------------------------------------------
// mulberry32 — Seedable 32-bit PRNG.  MUST be identical across all 4 languages.
//
// Returns a float in [0, 1) from a 32-bit internal state.
// ---------------------------------------------------------------------------
function makePrng(seed: number): () => number {
  let s = seed >>> 0;
  return function () {
    s |= 0;
    s = (s + 0x6d2b79f5) | 0;
    let t = Math.imul(s ^ (s >>> 15), 1 | s);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function padAccount(n: number): string {
  return "ACC" + n.toString().padStart(4, "0");
}

function randomAccount(rand: () => number): string {
  return padAccount(Math.floor(rand() * ACCOUNTS_COUNT) + 1);
}

function randomAmount(rand: () => number): string {
  // Range: 10.00 – 10000.00
  const amount = 10 + rand() * 9990;
  return amount.toFixed(2);
}

function randomTimestamp(rand: () => number, baseMs: number): number {
  // Spread across the year 2024 (≈ 366 days in milliseconds)
  const offsetMs = Math.floor(rand() * 366 * 24 * 60 * 60 * 1000);
  return baseMs + offsetMs;
}

function randomCategory(rand: () => number): string {
  return CATEGORIES[Math.floor(rand() * CATEGORIES.length)];
}

function randomPriority(rand: () => number): number {
  return Math.floor(rand() * 5) + 1;
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------
function main(): void {
  const args = process.argv.slice(2);
  const n = args.length > 0 ? parseInt(args[0], 10) : 10_000;
  const outputFile = args.length > 1 ? args[1] : null;

  if (isNaN(n) || n <= 0) {
    process.stderr.write("Usage: bun run scripts/generate-data.ts <positive-integer> [output-file]\n");
    process.exit(1);
  }

  const rand = makePrng(42); // fixed seed — identical across all languages
  // 2024-01-01T00:00:00Z in epoch milliseconds
  const base2024 = 1704067200000;

  const lines: string[] = ["id,amount,source_account,dest_account,timestamp,category,priority"];

  // Keep a small pool of recent rows to create intentional duplicates from
  const recentPool: { amount: string; source: string; dest: string; timestamp: number; category: string; priority: number }[] = [];
  const POOL_SIZE = 50;

  for (let i = 1; i <= n; i++) {
    const isDuplicate = rand() < 0.02 && recentPool.length > 0; // ~2% duplicates
    const isOverdraft = rand() < 0.01; // ~1% that will overdraft

    let amount: string;
    let source: string;
    let dest: string;
    let ts: number;
    let category: string;
    let priority: number;

    if (isDuplicate) {
      // Re-use a row from the recent pool (same amount/src/dst)
      const original = recentPool[Math.floor(rand() * recentPool.length)];
      // Shift timestamp by ≤ 2 seconds to stay within the 5-second dup window
      const shiftMs = Math.floor(rand() * 2_000); // 0-1999 ms
      ts = original.timestamp + shiftMs;
      amount = original.amount;
      source = original.source;
      dest = original.dest;
      category = original.category;
      priority = original.priority;
    } else if (isOverdraft) {
      // Use an unrealistically large amount to guarantee balance rejection
      amount = "60000.00";
      source = randomAccount(rand);
      dest = randomAccount(rand);
      ts = randomTimestamp(rand, base2024);
      category = randomCategory(rand);
      priority = randomPriority(rand);
    } else {
      amount = randomAmount(rand);
      source = randomAccount(rand);
      dest = randomAccount(rand);
      // Ensure source ≠ dest
      while (dest === source) dest = randomAccount(rand);
      ts = randomTimestamp(rand, base2024);
      category = randomCategory(rand);
      priority = randomPriority(rand);
    }

    lines.push(`${i},${amount},${source},${dest},${ts},${category},${priority}`);

    // Maintain the recent pool for duplicate generation
    if (!isDuplicate) {
      if (recentPool.length >= POOL_SIZE) {
        recentPool.shift();
      }
      recentPool.push({ amount, source, dest, timestamp: ts, category, priority });
    }
  }

  const content = lines.join("\n") + "\n";

  if (outputFile) {
    writeFileSync(outputFile, content);
    process.stderr.write(`Generated ${n.toLocaleString()} transactions → ${outputFile}\n`);
  } else {
    process.stdout.write(content);
  }
}

main();

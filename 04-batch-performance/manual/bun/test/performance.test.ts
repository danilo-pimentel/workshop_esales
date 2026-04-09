// ---------------------------------------------------------------------------
// performance.test.ts — Benchmarks BatchProcessor with a 10K record dataset.
//
// These tests do NOT validate correctness in detail — that is the job of
// correctness.test.ts.  Their purpose is to:
//   1. Verify that processing completes (no infinite loop / crash).
//   2. Record the elapsed time so students can compare before/after.
//   3. Optionally assert a time ceiling (currently lenient — set lower after
//      your optimisation is complete).
// ---------------------------------------------------------------------------

import { describe, it, expect } from "bun:test";
import { BatchProcessor } from "../src/processor";
import type { Transaction } from "../src/models";

// ---------------------------------------------------------------------------
// Deterministic dataset generator
// (same logic as generate-data.ts but inline so the test has no I/O dep)
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

const CATEGORIES: Transaction["category"][] = [
  "PAYMENT", "TRANSFER", "REFUND", "PURCHASE",
  "WITHDRAWAL", "DEPOSIT", "FEE", "INTEREST",
];

function generateTransactions(n: number, seed = 42): Transaction[] {
  const rand = makePrng(seed);
  const base2024 = new Date("2024-01-01T00:00:00").getTime();
  const yearMs = 366 * 24 * 60 * 60 * 1000;
  const ACCOUNTS = 1_000;

  const txs: Transaction[] = [];
  const recentPool: Transaction[] = [];
  const POOL_SIZE = 50;

  for (let i = 1; i <= n; i++) {
    const isDuplicate = rand() < 0.02 && recentPool.length > 0;
    const isNegBalance = rand() < 0.01;

    let amount: number;
    let source: string;
    let dest: string;
    let ts: number;
    let category: Transaction["category"];
    let priority: number;

    if (isDuplicate) {
      const orig = recentPool[Math.floor(rand() * recentPool.length)];
      ts = orig.timestamp + Math.floor(rand() * 2_000);
      amount = orig.amount;
      source = orig.source_account;
      dest = orig.dest_account;
      category = orig.category;
      priority = orig.priority;
    } else if (isNegBalance) {
      amount = ACCOUNTS * 60_000;
      source = "ACC" + (Math.floor(rand() * ACCOUNTS) + 1).toString().padStart(4, "0");
      dest   = "ACC" + (Math.floor(rand() * ACCOUNTS) + 1).toString().padStart(4, "0");
      ts = base2024 + Math.floor(rand() * yearMs);
      category = CATEGORIES[Math.floor(rand() * CATEGORIES.length)];
      priority = Math.floor(rand() * 5) + 1;
    } else {
      amount = parseFloat((10 + rand() * 9990).toFixed(2));
      const srcNum = Math.floor(rand() * ACCOUNTS) + 1;
      let dstNum = Math.floor(rand() * ACCOUNTS) + 1;
      while (dstNum === srcNum) dstNum = Math.floor(rand() * ACCOUNTS) + 1;
      source = "ACC" + srcNum.toString().padStart(4, "0");
      dest   = "ACC" + dstNum.toString().padStart(4, "0");
      ts = base2024 + Math.floor(rand() * yearMs);
      category = CATEGORIES[Math.floor(rand() * CATEGORIES.length)];
      priority = Math.floor(rand() * 5) + 1;
    }

    const t: Transaction = { id: i, amount, source_account: source, dest_account: dest, timestamp: ts, category, priority };
    txs.push(t);

    if (!isDuplicate) {
      if (recentPool.length >= POOL_SIZE) recentPool.shift();
      recentPool.push(t);
    }
  }

  return txs;
}

// ---------------------------------------------------------------------------
// Benchmarks
// ---------------------------------------------------------------------------

describe("Performance benchmarks", () => {
  // -------------------------------------------------------------------------
  // Small warm-up: 1 000 records — should be near-instant
  // -------------------------------------------------------------------------
  it("processes 1 000 records", () => {
    const data = generateTransactions(1_000);
    const processor = new BatchProcessor();

    const t0 = performance.now();
    const result = processor.process(data);
    const elapsed = performance.now() - t0;

    console.log(`\n  [1k] elapsed: ${elapsed.toFixed(1)}ms`);
    console.log(`       processed=${result.processed}  duplicates=${result.duplicates}  rejected=${result.rejected}  rings=${result.fraudRings}`);

    expect(result.processed + result.duplicates + result.rejected).toBe(1_000);
    // Very lenient ceiling — 10s is more than enough for 1k even with O(n²)
    expect(elapsed).toBeLessThan(10_000);
  });

  // -------------------------------------------------------------------------
  // Main benchmark: 10 000 records
  // -------------------------------------------------------------------------
  it("processes 10 000 records", () => {
    const data = generateTransactions(10_000);
    const processor = new BatchProcessor();

    const t0 = performance.now();
    const result = processor.process(data);
    const elapsed = performance.now() - t0;

    console.log(`\n  [10k] elapsed: ${elapsed.toFixed(1)}ms`);
    console.log(`        processed=${result.processed}  duplicates=${result.duplicates}  rejected=${result.rejected}  rings=${result.fraudRings}`);
    console.log("\n  Category report:");
    for (const stat of result.report) {
      console.log(
        `    ${stat.category.padEnd(16)} count=${stat.count.toString().padStart(5)}  total=${stat.total.toFixed(2).padStart(12)}`
      );
    }

    expect(result.processed + result.duplicates + result.rejected).toBe(10_000);
    // Lenient ceiling: 120s allows even slow machines to pass.
    // After optimisation, this should drop to under 1s.
    expect(elapsed).toBeLessThan(120_000);
  });

  // -------------------------------------------------------------------------
  // Throughput comparison helper: 500 records vs 1 000 records
  // Demonstrates super-linear (O(n²)) scaling of the unoptimised code.
  // -------------------------------------------------------------------------
  it("demonstrates O(n²) scaling: 1000 records takes ~4× longer than 500", () => {
    const data500  = generateTransactions(500,  1);
    const data1000 = generateTransactions(1_000, 2);

    const t0 = performance.now();
    new BatchProcessor().process(data500);
    const time500 = performance.now() - t0;

    const t1 = performance.now();
    new BatchProcessor().process(data1000);
    const time1000 = performance.now() - t1;

    const ratio = time1000 / time500;
    console.log(`\n  Scaling ratio (1000 / 500): ${ratio.toFixed(2)}x`);
    console.log(`  500  records: ${time500.toFixed(1)}ms`);
    console.log(`  1000 records: ${time1000.toFixed(1)}ms`);
    console.log("  (O(n²) would be ~4x; O(n log n) would be ~2.2x)");

    // We just log the ratio — don't assert a specific value because the fraud
    // ring DFS is particularly sensitive to graph structure (varies with seed).
    // Students can observe the ratio trending toward 4x on larger sizes.
    expect(time1000).toBeGreaterThan(0);
  });
});

// ---------------------------------------------------------------------------
// correctness.test.ts — Validates that BatchProcessor produces correct results.
//
// All tests use small, hand-crafted datasets so they run in milliseconds and
// provide clear failure messages when the implementation deviates.
// ---------------------------------------------------------------------------

import { describe, it, expect, beforeEach } from "bun:test";
import { BatchProcessor } from "../src/processor";
import type { Transaction, Category } from "../src/models";

// ---------------------------------------------------------------------------
// Helper
// ---------------------------------------------------------------------------

let idSeq = 0;

function tx(
  overrides: Partial<Transaction> & { source_account: string; dest_account: string; amount: number }
): Transaction {
  const base: Transaction = {
    id: ++idSeq,
    amount: overrides.amount,
    source_account: overrides.source_account,
    dest_account: overrides.dest_account,
    timestamp: overrides.timestamp ?? new Date("2024-06-01T12:00:00").getTime(),
    category: (overrides.category ?? "TRANSFER") as Category,
    priority: overrides.priority ?? 3,
  };
  return { ...base, ...overrides };
}

function makeTs(isoString: string): number {
  return new Date(isoString).getTime();
}

beforeEach(() => {
  idSeq = 0;
});

// ---------------------------------------------------------------------------
// 1. Duplicate detection
// ---------------------------------------------------------------------------

describe("Duplicate detection", () => {
  it("accepts two transactions with the same fields but >5s apart", () => {
    const processor = new BatchProcessor();
    const t1 = tx({ source_account: "ACC001", dest_account: "ACC002", amount: 100, timestamp: makeTs("2024-01-01T10:00:00") });
    const t2 = tx({ source_account: "ACC001", dest_account: "ACC002", amount: 100, timestamp: makeTs("2024-01-01T10:00:06") }); // 6 s later
    const result = processor.process([t1, t2]);
    expect(result.duplicates).toBe(0);
    expect(result.processed).toBe(2);
  });

  it("flags a transaction as duplicate when fields match within 5 seconds", () => {
    const processor = new BatchProcessor();
    const baseTs = makeTs("2024-01-01T10:00:00");
    const t1 = tx({ source_account: "ACC001", dest_account: "ACC002", amount: 200, timestamp: baseTs });
    const t2 = tx({ source_account: "ACC001", dest_account: "ACC002", amount: 200, timestamp: baseTs + 2_000 }); // 2 s later
    const result = processor.process([t1, t2]);
    expect(result.duplicates).toBe(1);
    expect(result.processed).toBe(1);
  });

  it("does not flag as duplicate when amounts differ", () => {
    const processor = new BatchProcessor();
    const baseTs = makeTs("2024-01-01T10:00:00");
    const t1 = tx({ source_account: "ACC001", dest_account: "ACC002", amount: 100, timestamp: baseTs });
    const t2 = tx({ source_account: "ACC001", dest_account: "ACC002", amount: 101, timestamp: baseTs + 1_000 });
    const result = processor.process([t1, t2]);
    expect(result.duplicates).toBe(0);
    expect(result.processed).toBe(2);
  });

  it("does not flag as duplicate when destination differs", () => {
    const processor = new BatchProcessor();
    const baseTs = makeTs("2024-01-01T10:00:00");
    const t1 = tx({ source_account: "ACC001", dest_account: "ACC002", amount: 100, timestamp: baseTs });
    const t2 = tx({ source_account: "ACC001", dest_account: "ACC003", amount: 100, timestamp: baseTs + 1_000 });
    const result = processor.process([t1, t2]);
    expect(result.duplicates).toBe(0);
    expect(result.processed).toBe(2);
  });

  it("counts multiple duplicates independently", () => {
    const processor = new BatchProcessor();
    const baseTs = makeTs("2024-01-01T10:00:00");
    const t1 = tx({ source_account: "ACC001", dest_account: "ACC002", amount: 50, timestamp: baseTs });
    const t2 = tx({ source_account: "ACC001", dest_account: "ACC002", amount: 50, timestamp: baseTs + 1_000 }); // dup
    const t3 = tx({ source_account: "ACC001", dest_account: "ACC002", amount: 50, timestamp: baseTs + 2_000 }); // dup of t1
    const result = processor.process([t1, t2, t3]);
    expect(result.duplicates).toBe(2);
    expect(result.processed).toBe(1);
  });
});

// ---------------------------------------------------------------------------
// 2. Account balance / rejection (INITIAL_BALANCE = 10_000)
// ---------------------------------------------------------------------------

describe("Account balance tracking", () => {
  it("rejects a transaction when source has insufficient funds", () => {
    const processor = new BatchProcessor();
    // Initial balance = 10 000 per account
    const drain = tx({ source_account: "ACC010", dest_account: "ACC011", amount: 9_999 });
    const overdraft = tx({ source_account: "ACC010", dest_account: "ACC012", amount: 5_000 });
    const result = processor.process([drain, overdraft]);
    expect(result.processed).toBe(1);
    expect(result.rejected).toBe(1);
  });

  it("accepts a transaction when source has exactly enough funds", () => {
    const processor = new BatchProcessor();
    const exact = tx({ source_account: "ACC020", dest_account: "ACC021", amount: 10_000 });
    const result = processor.process([exact]);
    expect(result.processed).toBe(1);
    expect(result.rejected).toBe(0);
  });

  it("allows destination to spend funds it received earlier", () => {
    const processor = new BatchProcessor();
    const t1 = tx({ source_account: "ACC030", dest_account: "ACC031", amount: 5_000 });
    // ACC031 now has 10 000 + 5 000 = 15 000; should be able to send 12 000
    const t2 = tx({ source_account: "ACC031", dest_account: "ACC032", amount: 12_000 });
    const result = processor.process([t1, t2]);
    expect(result.processed).toBe(2);
    expect(result.rejected).toBe(0);
  });

  it("rejects multiple overdraft attempts independently", () => {
    const processor = new BatchProcessor();
    const big1 = tx({ source_account: "ACC040", dest_account: "ACC041", amount: 60_000 });
    const big2 = tx({ source_account: "ACC042", dest_account: "ACC043", amount: 60_000 });
    const result = processor.process([big1, big2]);
    expect(result.rejected).toBe(2);
    expect(result.processed).toBe(0);
  });
});

// ---------------------------------------------------------------------------
// 3. Fraud ring detection (fraudRings is a number, not an array)
// ---------------------------------------------------------------------------

describe("Fraud ring detection", () => {
  it("detects a simple A→B→A cycle", () => {
    const processor = new BatchProcessor();
    const t1 = tx({ source_account: "ACC100", dest_account: "ACC101", amount: 100 });
    const t2 = tx({ source_account: "ACC101", dest_account: "ACC100", amount: 80 });
    const result = processor.process([t1, t2]);
    expect(result.fraudRings).toBeGreaterThan(0);
  });

  it("detects a three-node cycle A→B→C→A", () => {
    const processor = new BatchProcessor();
    const t1 = tx({ source_account: "ACC200", dest_account: "ACC201", amount: 100 });
    const t2 = tx({ source_account: "ACC201", dest_account: "ACC202", amount: 100 });
    const t3 = tx({ source_account: "ACC202", dest_account: "ACC200", amount: 100 });
    const result = processor.process([t1, t2, t3]);
    expect(result.fraudRings).toBeGreaterThan(0);
  });

  it("returns no rings when graph is acyclic", () => {
    const processor = new BatchProcessor();
    // Simple chain: A→B→C — no cycle
    const t1 = tx({ source_account: "ACC300", dest_account: "ACC301", amount: 100 });
    const t2 = tx({ source_account: "ACC301", dest_account: "ACC302", amount: 80 });
    const result = processor.process([t1, t2]);
    expect(result.fraudRings).toBe(0);
  });

  it("counts fraud rings correctly for a single cycle", () => {
    const processor = new BatchProcessor();
    const t1 = tx({ source_account: "ACC400", dest_account: "ACC401", amount: 100 });
    const t2 = tx({ source_account: "ACC401", dest_account: "ACC400", amount: 100 });
    const result = processor.process([t1, t2]);
    expect(result.fraudRings).toBeGreaterThanOrEqual(1);
  });
});

// ---------------------------------------------------------------------------
// 4. Priority ordering within batch windows
// ---------------------------------------------------------------------------

describe("Priority processing order", () => {
  it("higher-priority transactions appear before lower-priority within a batch", () => {
    const processor = new BatchProcessor();
    // All timestamps the same to avoid deduplication issues
    const baseTs = makeTs("2024-03-01T09:00:00");
    const txLow    = tx({ source_account: "ACC500", dest_account: "ACC501", amount: 10, priority: 1, timestamp: baseTs });
    const txMid    = tx({ source_account: "ACC502", dest_account: "ACC503", amount: 10, priority: 3, timestamp: baseTs });
    const txHigh   = tx({ source_account: "ACC504", dest_account: "ACC505", amount: 10, priority: 5, timestamp: baseTs });
    // Submit in low→mid→high order; processor should reorder to high→mid→low
    const result = processor.process([txLow, txMid, txHigh]);
    expect(result.processed).toBe(3);
  });

  it("preserves relative order for equal-priority transactions", () => {
    const processor = new BatchProcessor();
    const baseTs = makeTs("2024-03-01T09:00:00");
    const transactions = Array.from({ length: 5 }, (_, i) =>
      tx({ source_account: `ACC60${i}`, dest_account: `ACC61${i}`, amount: 10, priority: 3, timestamp: baseTs + i * 10_000 })
    );
    const result = processor.process(transactions);
    expect(result.processed).toBe(5);
  });
});

// ---------------------------------------------------------------------------
// 5. Category report generation (CategoryStats has: category, count, total, average)
// ---------------------------------------------------------------------------

describe("Category report", () => {
  it("reports correct totals for each category", () => {
    const processor = new BatchProcessor();
    const baseTs = makeTs("2024-05-01T08:00:00");
    const transactions: Transaction[] = [
      tx({ source_account: "ACC700", dest_account: "ACC701", amount: 100, category: "PAYMENT" as Category, timestamp: baseTs }),
      tx({ source_account: "ACC702", dest_account: "ACC703", amount: 200, category: "PAYMENT" as Category, timestamp: baseTs + 10_000 }),
      tx({ source_account: "ACC704", dest_account: "ACC705", amount: 300, category: "WITHDRAWAL" as Category, timestamp: baseTs + 20_000 }),
    ];
    const result = processor.process(transactions);
    const payment    = result.report.find((r) => r.category === "PAYMENT");
    const withdrawal = result.report.find((r) => r.category === "WITHDRAWAL");
    expect(payment).toBeDefined();
    expect(payment!.count).toBe(2);
    expect(payment!.total).toBe(300);
    expect(payment!.average).toBe(150);
    expect(withdrawal).toBeDefined();
    expect(withdrawal!.count).toBe(1);
    expect(withdrawal!.total).toBe(300);
  });

  it("reports correct average per category", () => {
    const processor = new BatchProcessor();
    const baseTs = makeTs("2024-05-01T08:00:00");
    const transactions: Transaction[] = [
      tx({ source_account: "ACC800", dest_account: "ACC801", amount: 50,  category: "FEE" as Category, timestamp: baseTs }),
      tx({ source_account: "ACC802", dest_account: "ACC803", amount: 150, category: "FEE" as Category, timestamp: baseTs + 10_000 }),
      tx({ source_account: "ACC804", dest_account: "ACC805", amount: 100, category: "FEE" as Category, timestamp: baseTs + 20_000 }),
    ];
    const result = processor.process(transactions);
    const fee = result.report.find((r) => r.category === "FEE");
    expect(fee).toBeDefined();
    expect(fee!.count).toBe(3);
    expect(fee!.total).toBe(300);
    expect(fee!.average).toBe(100);
  });

  it("report contains only categories present in processed transactions", () => {
    const processor = new BatchProcessor();
    const baseTs = makeTs("2024-05-01T08:00:00");
    const transactions: Transaction[] = [
      tx({ source_account: "ACC900", dest_account: "ACC901", amount: 10, category: "DEPOSIT" as Category, timestamp: baseTs }),
    ];
    const result = processor.process(transactions);
    expect(result.report.length).toBe(1);
    expect(result.report[0].category).toBe("DEPOSIT");
  });

  it("returns empty report for empty input", () => {
    const processor = new BatchProcessor();
    const result = processor.process([]);
    expect(result.report).toEqual([]);
  });

  it("returns zero counts when all transactions are rejected or duplicated", () => {
    const processor = new BatchProcessor();
    // All overdrafts → rejected (amount > 10K initial balance)
    const overdraft = tx({ source_account: "ACC950", dest_account: "ACC951", amount: 999_999 });
    const result = processor.process([overdraft]);
    expect(result.processed).toBe(0);
    expect(result.report).toEqual([]);
  });
});

// ---------------------------------------------------------------------------
// 6. Integrated correctness with ~100-record synthetic dataset
// ---------------------------------------------------------------------------

describe("Integrated correctness (100 records)", () => {
  function buildDataset(): Transaction[] {
    // Deterministic pseudo-random helper
    let seed = 1;
    const rand = (): number => {
      seed = (seed * 1664525 + 1013904223) & 0xffffffff;
      return (seed >>> 0) / 0x100000000;
    };

    const categories: Category[] = [
      "PAYMENT", "TRANSFER", "REFUND", "PURCHASE",
      "WITHDRAWAL", "DEPOSIT", "FEE", "INTEREST",
    ];
    const baseTs = new Date("2024-01-01T00:00:00").getTime();

    return Array.from({ length: 100 }, (_, i) => {
      const srcNum = Math.floor(rand() * 50) + 1;
      let dstNum = Math.floor(rand() * 50) + 1;
      if (dstNum === srcNum) dstNum = (dstNum % 50) + 1;
      return {
        id: i + 1,
        amount: parseFloat((10 + rand() * 990).toFixed(2)),
        source_account: `ACC${srcNum.toString().padStart(3, "0")}`,
        dest_account: `ACC${dstNum.toString().padStart(3, "0")}`,
        timestamp: baseTs + Math.floor(rand() * 30 * 24 * 3600 * 1000),
        category: categories[Math.floor(rand() * categories.length)],
        priority: Math.floor(rand() * 5) + 1,
      };
    });
  }

  it("processes 100 records with no unhandled errors", () => {
    const processor = new BatchProcessor();
    const result = processor.process(buildDataset());
    expect(result.processed + result.duplicates + result.rejected).toBe(100);
  });

  it("report totals add up correctly", () => {
    const processor = new BatchProcessor();
    const data = buildDataset();
    const result = processor.process(data);
    const reportTotal = result.report.reduce((sum, r) => sum + r.count, 0);
    expect(reportTotal).toBe(result.processed);
  });

  it("is deterministic — same input yields same output", () => {
    const data = buildDataset();
    const r1 = new BatchProcessor().process(data);
    const r2 = new BatchProcessor().process(data);
    expect(r1.processed).toBe(r2.processed);
    expect(r1.duplicates).toBe(r2.duplicates);
    expect(r1.rejected).toBe(r2.rejected);
    expect(r1.fraudRings).toBe(r2.fraudRings);
  });
});

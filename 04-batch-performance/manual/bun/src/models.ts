// ---------------------------------------------------------------------------
// models.ts — Domain types for the financial transaction batch processor
// ---------------------------------------------------------------------------

export type Category =
  | "PAYMENT"
  | "TRANSFER"
  | "REFUND"
  | "PURCHASE"
  | "WITHDRAWAL"
  | "DEPOSIT"
  | "FEE"
  | "INTEREST";

export interface Transaction {
  id: number;
  amount: number;
  source_account: string;
  dest_account: string;
  /** Unix timestamp in milliseconds */
  timestamp: number;
  category: Category;
  /** 1 (lowest) – 5 (highest) */
  priority: number;
}

export interface AccountBalance {
  accountId: string;
  balance: number;
}

export interface CategoryStats {
  category: string;
  count: number;
  total: number;
  average: number;
}

export interface ProcessingResult {
  processed: number;
  duplicates: number;
  rejected: number;
  fraudRings: number;
  report: CategoryStats[];
}

// ---------------------------------------------------------------------------
// CSV parsing helpers
// ---------------------------------------------------------------------------

/**
 * Parse a single CSV line into a Transaction object.
 * Expected header order: id,amount,source_account,dest_account,timestamp,category,priority
 */
export function parseTransaction(line: string): Transaction {
  const parts = line.split(",");
  if (parts.length !== 7) {
    throw new Error(`Invalid CSV line (expected 7 columns): "${line}"`);
  }
  const [id, amount, source_account, dest_account, timestamp, category, priority] = parts;
  return {
    id: parseInt(id, 10),
    amount: parseFloat(amount),
    source_account: source_account.trim(),
    dest_account: dest_account.trim(),
    timestamp: parseInt(timestamp.trim(), 10),
    category: category.trim() as Category,
    priority: parseInt(priority, 10),
  };
}

/**
 * Parse full CSV text (including optional header row) into Transaction[].
 */
export function parseCSV(csvText: string): Transaction[] {
  const lines = csvText.trim().split("\n");
  const transactions: Transaction[] = [];

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("id,")) continue; // skip header / blank
    try {
      transactions.push(parseTransaction(trimmed));
    } catch {
      // skip malformed rows silently
    }
  }

  return transactions;
}

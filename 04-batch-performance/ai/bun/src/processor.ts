// ---------------------------------------------------------------------------
// processor.ts — BatchProcessor with 5 processing stages.
//
// The code is CORRECT; the task is to make it FAST without
// changing business rules or test expectations.
//
// ---------------------------------------------------------------------------

import { LinkedList, AdjacencyMatrix } from "./data-structures";
import type { Transaction, AccountBalance, CategoryStats, ProcessingResult } from "./models";

// Initial balance granted to every account that appears in the data set
const INITIAL_BALANCE = 10_000;

// Two transactions are considered duplicates if they share the same source,
// destination and amount, and their timestamps differ by less than 5 seconds.
const DUPLICATE_WINDOW_MS = 5_000;

export class BatchProcessor {

  // -----------------------------------------------------------------------
  // Public entry point — runs all 5 stages sequentially
  // -----------------------------------------------------------------------
  process(transactions: Transaction[]): ProcessingResult {
    const total = transactions.length;

    // Stage 1: Duplicate detection
    const s1 = performance.now();
    const { unique, duplicateCount } = this.detectDuplicates(transactions);
    process.stdout.write(`\r  [1/5] Detectando duplicatas... 100% (${total}/${total}) ${((performance.now() - s1) / 1000).toFixed(3)}s\n`);

    // Stage 2: Account balance validation
    const s2 = performance.now();
    const { accepted, rejectedCount } = this.validateBalances(unique);
    process.stdout.write(`\r  [2/5] Validando saldos... 100% (${unique.length}/${unique.length}) ${((performance.now() - s2) / 1000).toFixed(3)}s\n`);

    // Stage 3: Fraud ring detection
    const s3 = performance.now();
    const fraudRings = this.detectFraudRings(accepted);
    process.stdout.write(`\r  [3/5] Detectando aneis de fraude... 100% ${((performance.now() - s3) / 1000).toFixed(3)}s\n`);

    // Stage 4: Priority sorting
    const s4 = performance.now();
    const sorted = this.sortByPriority(accepted);
    process.stdout.write(`\r  [4/5] Ordenando por prioridade... 100% (${accepted.length}/${accepted.length}) ${((performance.now() - s4) / 1000).toFixed(3)}s\n`);

    // Stage 5: Category report
    const s5 = performance.now();
    const report = this.generateReport(sorted);
    process.stdout.write(`\r  [5/5] Gerando relatorio por categoria... 100% ${((performance.now() - s5) / 1000).toFixed(3)}s\n`);

    return {
      processed: accepted.length,
      duplicates: duplicateCount,
      rejected: rejectedCount,
      fraudRings,
      report,
    };
  }

  // -----------------------------------------------------------------------
  // Stage 1 — Duplicate Detection
  // -----------------------------------------------------------------------
  private detectDuplicates(transactions: Transaction[]): { unique: Transaction[]; duplicateCount: number } {
    const seen = new LinkedList<Transaction>();
    const unique: Transaction[] = [];
    let duplicateCount = 0;

    const total = transactions.length;
    const interval = Math.max(Math.floor(total / 20), 1);

    for (let i = 0; i < total; i++) {
      if (i % interval === 0) {
        process.stdout.write(`\r  [1/5] Detectando duplicatas... ${Math.floor(i * 100 / total)}% (${i}/${total})`);
      }

      const tx = transactions[i];

      // O(n) scan of the LinkedList for each transaction
      const isDup = seen.find(
        (prev) =>
          prev.source_account === tx.source_account &&
          prev.dest_account === tx.dest_account &&
          prev.amount === tx.amount &&
          Math.abs(prev.timestamp - tx.timestamp) < DUPLICATE_WINDOW_MS
      );

      if (isDup !== undefined) {
        duplicateCount++;
      } else {
        unique.push(tx);
        seen.append(tx);
      }
    }

    return { unique, duplicateCount };
  }

  // -----------------------------------------------------------------------
  // Stage 2 — Account Balance Validation
  // -----------------------------------------------------------------------
  private validateBalances(transactions: Transaction[]): { accepted: Transaction[]; rejectedCount: number } {
    const balances = new LinkedList<AccountBalance>();
    const accepted: Transaction[] = [];
    let rejectedCount = 0;

    const total = transactions.length;
    const interval = Math.max(Math.floor(total / 20), 1);

    for (let i = 0; i < total; i++) {
      if (i % interval === 0) {
        process.stdout.write(`\r  [2/5] Validando saldos... ${Math.floor(i * 100 / total)}% (${i}/${total})`);
      }

      const tx = transactions[i];

      // Ensure both accounts exist (O(n) find + O(n) insertSorted each)
      this.ensureAccount(balances, tx.source_account);
      this.ensureAccount(balances, tx.dest_account);

      // Check source has sufficient funds — O(n) scan
      const sourceEntry = balances.find((b) => b.accountId === tx.source_account);
      if (!sourceEntry || sourceEntry.balance < tx.amount) {
        rejectedCount++;
        continue;
      }

      // Debit source — O(n) scan
      balances.update(
        (b) => b.accountId === tx.source_account,
        (b) => ({ ...b, balance: b.balance - tx.amount })
      );

      // Credit destination — O(n) scan
      balances.update(
        (b) => b.accountId === tx.dest_account,
        (b) => ({ ...b, balance: b.balance + tx.amount })
      );

      accepted.push(tx);
    }

    return { accepted, rejectedCount };
  }

  /** Lazily initialise an account with INITIAL_BALANCE (sorted insert, O(n)). */
  private ensureAccount(balances: LinkedList<AccountBalance>, accountId: string): void {
    const existing = balances.find((b) => b.accountId === accountId);
    if (!existing) {
      balances.insertSorted(
        { accountId, balance: INITIAL_BALANCE },
        (a, b) => a.accountId.localeCompare(b.accountId)
      );
    }
  }

  // -----------------------------------------------------------------------
  // Stage 3 — Fraud Ring Detection
  // -----------------------------------------------------------------------
  private detectFraudRings(transactions: Transaction[]): number {
    if (transactions.length === 0) return 0;

    // Collect unique accounts
    const accountSet = new Set<string>();
    for (const tx of transactions) {
      accountSet.add(tx.source_account);
      accountSet.add(tx.dest_account);
    }
    const accounts = [...accountSet];

    // Build adjacency matrix — O(V²) allocation
    const graph = new AdjacencyMatrix(accounts);
    for (const tx of transactions) {
      graph.addEdge(tx.source_account, tx.dest_account);
    }

    let ringCount = 0;
    const globalVisited = new Set<string>();
    const totalV = accounts.length;
    const intervalV = Math.max(Math.floor(totalV / 20), 1);

    // DFS from every node — O(V × (V+E)) total
    for (let vi = 0; vi < totalV; vi++) {
      if (vi % intervalV === 0) {
        process.stdout.write(`\r  [3/5] Detectando aneis de fraude... ${Math.floor(vi * 100 / totalV)}% (${vi}/${totalV})`);
      }
      const startNode = accounts[vi];
      if (globalVisited.has(startNode)) continue;

      const visited = new Set<string>();
      const path: string[] = [];
      const ringsFoundInThisDfs = new Set<string>();

      const dfs = (node: string): void => {
        visited.add(node);
        path.push(node);

        // O(V) scan of the matrix row for this node
        const neighbours = graph.neighbours(node);
        for (const neighbour of neighbours) {
          if (!visited.has(neighbour)) {
            dfs(neighbour);
          } else {
            // Found a back-edge → cycle detected
            const cycleStart = path.indexOf(neighbour);
            if (cycleStart !== -1) {
              const cycle = path.slice(cycleStart);
              const cycleKey = [...cycle].sort().join(",");
              if (!ringsFoundInThisDfs.has(cycleKey)) {
                ringsFoundInThisDfs.add(cycleKey);
                ringCount++;
              }
            }
          }
        }

        path.pop();
        globalVisited.add(node);
      };

      dfs(startNode);
    }

    return ringCount;
  }

  // -----------------------------------------------------------------------
  // Stage 4 — Priority Sorting
  // -----------------------------------------------------------------------
  private sortByPriority(transactions: Transaction[]): Transaction[] {
    const sorted = new LinkedList<Transaction>();
    const total = transactions.length;
    const interval = Math.max(Math.floor(total / 20), 1);

    for (let i = 0; i < total; i++) {
      if (i % interval === 0) {
        process.stdout.write(`\r  [4/5] Ordenando por prioridade... ${Math.floor(i * 100 / total)}% (${i}/${total})`);
      }
      // Higher priority number = processed first (descending sort)
      sorted.insertSorted(transactions[i], (a, b) => b.priority - a.priority);
    }

    return sorted.toArray();
  }

  // -----------------------------------------------------------------------
  // Stage 5 — Category Report
  // -----------------------------------------------------------------------
  private generateReport(transactions: Transaction[]): CategoryStats[] {
    if (transactions.length === 0) return [];

    // Collect unique categories with a linear scan
    const uniqueCategories = [...new Set(transactions.map((t) => t.category))].sort();

    const stats: CategoryStats[] = [];
    const totalCat = uniqueCategories.length;

    // For each category, do a full second pass — O(n) per category
    for (let ci = 0; ci < totalCat; ci++) {
      process.stdout.write(`\r  [5/5] Gerando relatorio por categoria... ${Math.floor(ci * 100 / totalCat)}% (${ci}/${totalCat})`);
      const cat = uniqueCategories[ci];
      const filtered = transactions.filter((t) => t.category === cat);

      let total = 0;
      for (const t of filtered) {
        total += t.amount;
      }

      stats.push({
        category: cat,
        count: filtered.length,
        total: parseFloat(total.toFixed(2)),
        average: parseFloat((total / filtered.length).toFixed(2)),
      });
    }

    return stats;
  }
}

// ---------------------------------------------------------------------------
// data-structures.ts — Intentionally naive data structures used to introduce
//                      O(n²) performance characteristics.
//
// TRAINING NOTE: These structures are correct but deliberately slow.
//   - LinkedList has no random access; every lookup is O(n).
//   - AdjacencyMatrix uses a 2-D array (V×V) and wastes memory for sparse graphs.
//   Both should be replaced with Maps / Sets / adjacency-lists during the
//   optimisation exercise.
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// LinkedList
// ---------------------------------------------------------------------------

export class LinkedListNode<T> {
  value: T;
  next: LinkedListNode<T> | null = null;

  constructor(value: T) {
    this.value = value;
  }
}

export class LinkedList<T> {
  head: LinkedListNode<T> | null = null;
  size: number = 0;

  // O(n) — must walk to the tail before appending
  append(value: T): void {
    const node = new LinkedListNode(value);
    if (this.head === null) {
      this.head = node;
    } else {
      let current = this.head;
      while (current.next !== null) {
        current = current.next;
      }
      current.next = node;
    }
    this.size++;
  }

  // O(n) — linear scan every time
  find(predicate: (val: T) => boolean): T | undefined {
    let current = this.head;
    while (current !== null) {
      if (predicate(current.value)) {
        return current.value;
      }
      current = current.next;
    }
    return undefined;
  }

  // O(n) — iterate and collect
  toArray(): T[] {
    const result: T[] = [];
    let current = this.head;
    while (current !== null) {
      result.push(current.value);
      current = current.next;
    }
    return result;
  }

  /**
   * Insertion-sort style insert.  Walks the list to find the correct position,
   * then splices in the new node.  O(n) per call → O(n²) overall for n inserts.
   */
  insertSorted(value: T, comparator: (a: T, b: T) => number): void {
    const node = new LinkedListNode(value);

    // New node belongs at the head
    if (this.head === null || comparator(value, this.head.value) <= 0) {
      node.next = this.head;
      this.head = node;
      this.size++;
      return;
    }

    // Walk until we find where the new node should sit
    let current: LinkedListNode<T> = this.head;
    while (current.next !== null && comparator(value, current.next.value) > 0) {
      current = current.next;
    }
    node.next = current.next;
    current.next = node;
    this.size++;
  }

  /**
   * Update the first node that satisfies the predicate using the updater fn.
   * O(n) scan.
   */
  update(predicate: (val: T) => boolean, updater: (val: T) => T): boolean {
    let current = this.head;
    while (current !== null) {
      if (predicate(current.value)) {
        current.value = updater(current.value);
        return true;
      }
      current = current.next;
    }
    return false;
  }
}

// ---------------------------------------------------------------------------
// AdjacencyMatrix
// ---------------------------------------------------------------------------

/**
 * A graph represented as a V×V boolean matrix.
 *
 * TRAINING NOTE: Allocates O(V²) memory even for sparse graphs.  For typical
 * financial transfer graphs (few edges relative to V) an adjacency list would
 * be far more efficient.
 */
export class AdjacencyMatrix {
  private readonly nodeIndex: Map<string, number>;
  private readonly nodes: string[];
  /** Row-major V×V matrix stored as a flat array for cache friendliness */
  private readonly matrix: Uint8Array;
  readonly size: number;

  constructor(nodeLabels: string[]) {
    this.nodes = [...nodeLabels];
    this.size = nodeLabels.length;
    this.nodeIndex = new Map(nodeLabels.map((label, i) => [label, i]));
    // Allocate V² bytes (0 = no edge, 1 = edge exists)
    this.matrix = new Uint8Array(this.size * this.size);
  }

  private idx(node: string): number {
    const i = this.nodeIndex.get(node);
    if (i === undefined) throw new Error(`Unknown node: ${node}`);
    return i;
  }

  addEdge(from: string, to: string): void {
    const r = this.idx(from);
    const c = this.idx(to);
    this.matrix[r * this.size + c] = 1;
  }

  hasEdge(from: string, to: string): boolean {
    const r = this.idx(from);
    const c = this.idx(to);
    return this.matrix[r * this.size + c] === 1;
  }

  /** Returns node labels for all direct neighbours of `from` — O(V) scan */
  neighbours(from: string): string[] {
    const r = this.idx(from);
    const result: string[] = [];
    for (let c = 0; c < this.size; c++) {
      if (this.matrix[r * this.size + c] === 1) {
        result.push(this.nodes[c]);
      }
    }
    return result;
  }

  getNodes(): string[] {
    return [...this.nodes];
  }
}

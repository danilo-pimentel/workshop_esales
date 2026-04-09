<?php

declare(strict_types=1);

namespace BatchProcessor\DataStructures;

/**
 * Custom singly-linked list.
 *
 * This is an intentionally simple, O(n) implementation.
 * All operations traverse the entire chain – this is a deliberate design choice
 * to create measurable performance bottlenecks when the list grows large.
 *
 * DO NOT replace with SplDoublyLinkedList or array-based structures.
 */
final class LinkedList
{
    private ?Node $head = null;
    private int   $size = 0;

    // -------------------------------------------------------------------------
    // append  O(n) – walks to tail every time
    // -------------------------------------------------------------------------

    /**
     * Append a value at the end of the list.
     * Complexity: O(n) – must walk to the tail node.
     */
    public function append(mixed $value): void
    {
        $newNode = new Node($value);

        if ($this->head === null) {
            $this->head = $newNode;
            $this->size++;
            return;
        }

        // Walk to the last node – O(n)
        $current = $this->head;
        while ($current->next !== null) {
            $current = $current->next;
        }
        $current->next = $newNode;
        $this->size++;
    }

    // -------------------------------------------------------------------------
    // find  O(n)
    // -------------------------------------------------------------------------

    /**
     * Return the first value that satisfies $predicate, or null.
     * Complexity: O(n).
     */
    public function find(callable $predicate): mixed
    {
        $current = $this->head;
        while ($current !== null) {
            if ($predicate($current->value)) {
                return $current->value;
            }
            $current = $current->next;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // toArray  O(n)
    // -------------------------------------------------------------------------

    /**
     * Materialise all values into a plain PHP array.
     * Complexity: O(n).
     */
    public function toArray(): array
    {
        $result  = [];
        $current = $this->head;
        while ($current !== null) {
            $result[] = $current->value;
            $current  = $current->next;
        }
        return $result;
    }

    // -------------------------------------------------------------------------
    // insertSorted  O(n) per call  →  O(n²) to build a sorted list of n items
    // -------------------------------------------------------------------------

    /**
     * Insert $value into the list maintaining sorted order defined by $comparator.
     *
     * $comparator($a, $b) returns:
     *   negative  → $a comes before $b
     *   zero      → equal
     *   positive  → $a comes after $b
     *
     * Complexity per call: O(n) – linear scan to find the insertion point.
     * Building a fully sorted list of n items therefore costs O(n²) overall.
     */
    public function insertSorted(mixed $value, callable $comparator): void
    {
        $newNode = new Node($value);

        // Insert before head when list is empty or value belongs at the front
        if ($this->head === null || $comparator($value, $this->head->value) <= 0) {
            $newNode->next = $this->head;
            $this->head    = $newNode;
            $this->size++;
            return;
        }

        // Walk until we find the right spot – O(n)
        $current = $this->head;
        while ($current->next !== null && $comparator($value, $current->next->value) > 0) {
            $current = $current->next;
        }
        $newNode->next  = $current->next;
        $current->next  = $newNode;
        $this->size++;
    }

    // -------------------------------------------------------------------------
    // count  O(1) – maintained via $size counter
    // -------------------------------------------------------------------------

    /**
     * Return the number of elements in the list.
     * Complexity: O(1) – maintained as a counter.
     */
    public function count(): int
    {
        return $this->size;
    }

    // -------------------------------------------------------------------------
    // forEach helper
    // -------------------------------------------------------------------------

    /**
     * Execute $callback for every value in the list.
     * Complexity: O(n).
     */
    public function forEach(callable $callback): void
    {
        $current = $this->head;
        while ($current !== null) {
            $callback($current->value);
            $current = $current->next;
        }
    }
}

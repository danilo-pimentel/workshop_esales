package com.treinamento.batch.datastructures

/**
 * Singly-linked list node.
 *
 * Kept intentionally public so [LinkedList] and external helpers can
 * traverse the chain directly without going through the list API.
 */
class Node<T>(var value: T, var next: Node<T>? = null)

/**
 * Custom singly-linked list.
 *
 * INTENTIONAL PERFORMANCE NOTE
 * ─────────────────────────────
 * Every operation here is O(n):
 *   - [find]         – full sequential scan
 *   - [insertSorted] – full sequential scan to locate insertion point
 *   - [size]         – traverses every node on each call (no cached counter)
 *   - [toList]       – O(n) traversal
 *   - [append]       – O(n) walk to tail (no tail pointer kept)
 *
 * These properties are exploited in [com.treinamento.batch.BatchProcessor]
 * to produce measurable O(n²) bottlenecks.
 */
class LinkedList<T> {

    /** Head of the chain; null when the list is empty. */
    var head: Node<T>? = null
        private set

    // ── Basic operations ──────────────────────────────────────────────────

    /**
     * Appends [value] to the tail of the list.
     * O(n) – walks the entire chain to find the last node.
     */
    fun append(value: T) {
        val newNode = Node(value)
        val current = head
        if (current == null) {
            head = newNode
            return
        }
        // Walk all the way to the end – O(n) intentionally (no tail pointer).
        var tail: Node<T> = current
        while (tail.next != null) {
            tail = tail.next!!
        }
        tail.next = newNode
    }

    /**
     * Returns the first element that satisfies [predicate], or null.
     * O(n) – sequential scan of every node.
     */
    fun find(predicate: (T) -> Boolean): T? {
        var current = head
        while (current != null) {
            if (predicate(current.value)) return current.value
            current = current.next
        }
        return null
    }

    /**
     * Materialises the list as a [kotlin.collections.List].
     * O(n).
     */
    fun toList(): List<T> {
        val result = mutableListOf<T>()
        var current = head
        while (current != null) {
            result.add(current.value)
            current = current.next
        }
        return result
    }

    /**
     * Inserts [value] into the sorted position determined by [comparator].
     *
     * Assumes the list is already sorted. Scans from the head to locate the
     * correct insertion point – O(n) per call, making repeated calls O(n²).
     */
    fun insertSorted(value: T, comparator: Comparator<T>) {
        val newNode = Node(value)

        // Insert before head if the list is empty or value belongs at the front.
        if (head == null || comparator.compare(value, head!!.value) <= 0) {
            newNode.next = head
            head = newNode
            return
        }

        // Scan to find the node just before where value should land – O(n).
        var current = head!!
        while (current.next != null &&
            comparator.compare(value, current.next!!.value) > 0
        ) {
            current = current.next!!
        }
        newNode.next = current.next
        current.next = newNode
    }

    /**
     * Returns the number of elements.
     * O(n) – no cached counter is kept deliberately.
     */
    fun size(): Int {
        var count = 0
        var current = head
        while (current != null) {
            count++
            current = current.next
        }
        return count
    }

    /**
     * Removes all elements matching [predicate].
     * O(n).
     */
    fun removeIf(predicate: (T) -> Boolean) {
        // Skip matching nodes at the head.
        while (head != null && predicate(head!!.value)) {
            head = head!!.next
        }
        var current = head ?: return
        while (current.next != null) {
            if (predicate(current.next!!.value)) {
                current.next = current.next!!.next
            } else {
                current = current.next!!
            }
        }
    }

    /** Returns true when the list contains no elements. */
    fun isEmpty(): Boolean = head == null

    override fun toString(): String = toList().toString()
}

package com.treinamento.batch.datastructures;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Custom singly-linked list.
 *
 * <p><strong>Intentional performance characteristics:</strong>
 * Every method that searches or inserts by position is O(n). There is no
 * index, no hash table, and no skip structure — just a plain chain of
 * {@link Node} objects traversed from head to tail on every operation.
 *
 * <p>This is the <em>correct</em> but <em>slow</em> implementation that
 * the exercise asks you to replace with an efficient alternative.
 *
 * @param <T> type of elements stored in this list
 */
public class LinkedList<T> {

    // -------------------------------------------------------------------------
    // Inner node class
    // -------------------------------------------------------------------------

    /** A single link in the chain. */
    public static class Node<T> {
        public T data;
        public Node<T> next;

        public Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private Node<T> head;
    private int size;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Creates an empty list. */
    public LinkedList() {
        this.head = null;
        this.size = 0;
    }

    // -------------------------------------------------------------------------
    // Core operations — all O(n) by design
    // -------------------------------------------------------------------------

    /**
     * Appends {@code element} at the tail of the list.
     *
     * <p>O(n) — walks the entire chain to reach the last node.
     * An efficient implementation would keep a {@code tail} pointer and make
     * this O(1).
     */
    public void append(T element) {
        Node<T> newNode = new Node<>(element);
        if (head == null) {
            head = newNode;
        } else {
            Node<T> current = head;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
        size++;
    }

    /**
     * Returns the first element matching {@code predicate}, or {@code null}
     * if none is found.
     *
     * <p>O(n) — unavoidable for an unsorted list without an index.
     */
    public T find(Predicate<T> predicate) {
        Node<T> current = head;
        while (current != null) {
            if (predicate.test(current.data)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    /**
     * Returns all elements as an array.
     *
     * <p>O(n) — one pass to collect elements. The array type is
     * {@code Object[]} because Java generics do not allow {@code new T[size]}.
     * Callers must cast the result.
     */
    @SuppressWarnings("unchecked")
    public T[] toArray() {
        Object[] arr = new Object[size];
        Node<T> current = head;
        int i = 0;
        while (current != null) {
            arr[i++] = current.data;
            current = current.next;
        }
        return (T[]) arr;
    }

    /**
     * Inserts {@code element} into the list so that the list remains sorted
     * according to {@code comparator}.
     *
     * <p>O(n) — must scan from head to find the insertion point. Combined
     * with repeated calls this produces <strong>O(n²)</strong> behaviour for
     * building a fully-sorted list (insertion sort).
     */
    public void insertSorted(T element, Comparator<T> comparator) {
        Node<T> newNode = new Node<>(element);

        // Insert before head if list is empty or element is the smallest
        if (head == null || comparator.compare(element, head.data) <= 0) {
            newNode.next = head;
            head = newNode;
            size++;
            return;
        }

        Node<T> current = head;
        while (current.next != null
                && comparator.compare(element, current.next.data) > 0) {
            current = current.next;
        }
        newNode.next = current.next;
        current.next = newNode;
        size++;
    }

    /**
     * Removes the first element matching {@code predicate}.
     *
     * <p>O(n) — linear scan.
     *
     * @return {@code true} if an element was removed, {@code false} otherwise
     */
    public boolean remove(Predicate<T> predicate) {
        if (head == null) return false;

        if (predicate.test(head.data)) {
            head = head.next;
            size--;
            return true;
        }

        Node<T> current = head;
        while (current.next != null) {
            if (predicate.test(current.next.data)) {
                current.next = current.next.next;
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Returns the first element of the list without removing it.
     *
     * @throws NoSuchElementException if the list is empty
     */
    public T peek() {
        if (head == null) throw new NoSuchElementException("List is empty");
        return head.data;
    }

    /**
     * Removes and returns the first element of the list.
     *
     * @throws NoSuchElementException if the list is empty
     */
    public T poll() {
        if (head == null) throw new NoSuchElementException("List is empty");
        T data = head.data;
        head = head.next;
        size--;
        return data;
    }

    /** Returns the number of elements in this list. O(1). */
    public int size() {
        return size;
    }

    /** Returns {@code true} if this list contains no elements. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Provides access to the head node for iteration. */
    public Node<T> getHead() {
        return head;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<T> current = head;
        while (current != null) {
            sb.append(current.data);
            if (current.next != null) sb.append(", ");
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }
}

package com.treinamento.batch.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graph represented as a V x V adjacency matrix (2-D boolean array).
 *
 * <p><strong>Intentional performance characteristics:</strong>
 * <ul>
 *   <li>Space: O(V^2) -- always allocates a full V x V matrix regardless of edge
 *       density. A sparse graph wastes most of this space.</li>
 *   <li>{@link #getNeighbors(String)}: O(V) -- must scan an entire row even when
 *       the node has only a handful of edges. An adjacency-list representation
 *       would return neighbours in O(degree) time.</li>
 * </ul>
 */
public class AdjacencyMatrix {

    private final Map<String, Integer> nodeIndex;
    private final String[] nodes;
    private final boolean[][] matrix;
    private final int size;

    /**
     * Constructs an empty directed graph with the given node labels.
     *
     * @param nodeLabels array of unique node label strings
     */
    public AdjacencyMatrix(String[] nodeLabels) {
        this.size = nodeLabels.length;
        this.nodes = nodeLabels.clone();
        this.nodeIndex = new HashMap<>();
        for (int i = 0; i < size; i++) {
            nodeIndex.put(nodeLabels[i], i);
        }
        this.matrix = new boolean[size][size];
    }

    private int idx(String node) {
        Integer i = nodeIndex.get(node);
        if (i == null) throw new IllegalArgumentException("Unknown node: " + node);
        return i;
    }

    /**
     * Adds a directed edge from {@code from} to {@code to}.
     */
    public void addEdge(String from, String to) {
        matrix[idx(from)][idx(to)] = true;
    }

    /**
     * Returns {@code true} if there is a directed edge from {@code from} to
     * {@code to}.
     */
    public boolean hasEdge(String from, String to) {
        return matrix[idx(from)][idx(to)];
    }

    /**
     * Returns node labels for all direct neighbours of {@code from} -- O(V) scan.
     */
    public String[] getNeighbors(String from) {
        int r = idx(from);
        List<String> result = new ArrayList<>();
        for (int c = 0; c < size; c++) {
            if (matrix[r][c]) {
                result.add(nodes[c]);
            }
        }
        return result.toArray(new String[0]);
    }

    /**
     * Returns all node labels.
     */
    public String[] getNodes() {
        return nodes.clone();
    }

    /**
     * Returns the number of nodes in the graph.
     */
    public int getSize() {
        return size;
    }
}

package com.treinamento.batch.datastructures

/**
 * A graph represented as a V x V boolean matrix.
 *
 * TRAINING NOTE: Allocates O(V^2) memory even for sparse graphs. For typical
 * financial transfer graphs (few edges relative to V) an adjacency list would
 * be far more efficient.
 */
class AdjacencyMatrix(nodeLabels: Array<String>) {
    private val nodeIndex: Map<String, Int> = nodeLabels.withIndex().associate { (i, label) -> label to i }
    private val nodes: Array<String> = nodeLabels.copyOf()
    private val matrix: BooleanArray = BooleanArray(nodeLabels.size * nodeLabels.size)
    val size: Int = nodeLabels.size

    private fun idx(node: String): Int =
        nodeIndex[node] ?: throw IllegalArgumentException("Unknown node: $node")

    fun addEdge(from: String, to: String) {
        matrix[idx(from) * size + idx(to)] = true
    }

    fun hasEdge(from: String, to: String): Boolean =
        matrix[idx(from) * size + idx(to)]

    /** Returns node labels for all direct neighbours of `from` -- O(V) scan */
    fun getNeighbors(from: String): List<String> {
        val r = idx(from)
        val result = mutableListOf<String>()
        for (c in 0 until size) {
            if (matrix[r * size + c]) result.add(nodes[c])
        }
        return result
    }

    fun getNodes(): Array<String> = nodes.copyOf()
}

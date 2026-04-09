<?php

declare(strict_types=1);

namespace BatchProcessor\DataStructures;

/**
 * A graph represented as a V x V boolean matrix.
 *
 * TRAINING NOTE: Allocates O(V^2) memory even for sparse graphs.  For typical
 * financial transfer graphs (few edges relative to V) an adjacency list would
 * be far more efficient.
 */
final class AdjacencyMatrix
{
    /** @var array<string, int> string -> int */
    private array $nodeIndex;

    /** @var string[] int -> string */
    private array $nodes;

    /** @var bool[] flat array (row-major) */
    private array $matrix;

    private int $size;

    /**
     * @param string[] $nodeLabels
     */
    public function __construct(array $nodeLabels)
    {
        $this->size = count($nodeLabels);
        $this->nodes = array_values($nodeLabels);
        $this->nodeIndex = array_flip($this->nodes);
        $this->matrix = array_fill(0, $this->size * $this->size, false);
    }

    private function idx(string $node): int
    {
        if (!isset($this->nodeIndex[$node])) {
            throw new \InvalidArgumentException("Unknown node: $node");
        }
        return $this->nodeIndex[$node];
    }

    public function addEdge(string $from, string $to): void
    {
        $this->matrix[$this->idx($from) * $this->size + $this->idx($to)] = true;
    }

    public function hasEdge(string $from, string $to): bool
    {
        return $this->matrix[$this->idx($from) * $this->size + $this->idx($to)];
    }

    /**
     * Returns node labels for all direct neighbours of $from -- O(V) scan.
     *
     * @return string[]
     */
    public function getNeighbors(string $from): array
    {
        $r = $this->idx($from);
        $result = [];
        for ($c = 0; $c < $this->size; $c++) {
            if ($this->matrix[$r * $this->size + $c]) {
                $result[] = $this->nodes[$c];
            }
        }
        return $result;
    }

    /** @return string[] */
    public function getNodes(): array
    {
        return $this->nodes;
    }

    public function getSize(): int
    {
        return $this->size;
    }
}

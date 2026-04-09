<?php

declare(strict_types=1);

namespace BatchProcessor\DataStructures;

/**
 * Internal node for the custom LinkedList.
 *
 * Kept as a separate class (rather than a nested class) because PHP does not
 * support inner/nested classes.  The Node class is not part of the public API.
 */
final class Node
{
    public ?Node $next = null;

    public function __construct(
        public mixed $value,
    ) {}
}

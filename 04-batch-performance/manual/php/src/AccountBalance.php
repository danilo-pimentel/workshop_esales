<?php

declare(strict_types=1);

namespace BatchProcessor;

/**
 * Mutable account balance record stored in LinkedList nodes.
 * Using a class (reference type) so that find() returns a reference we can mutate.
 */
class AccountBalance
{
    public string $accountId;
    public float $balance;

    public function __construct(string $accountId, float $balance)
    {
        $this->accountId = $accountId;
        $this->balance = $balance;
    }
}

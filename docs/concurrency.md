# FlashSale Concurrency Engineering

## Problem

The inventory reservation operation initially uses a simple
read-check-update approach.

The system must guarantee:

> Inventory must never be oversold, even when many users
> attempt to purchase the same product simultaneously.

## Baseline Implementation

Current flow:

Request
→ Read inventory
→ Check available quantity
→ Decrease available quantity
→ Increase reserved quantity
→ Save

This implementation has not yet been made concurrency-safe.

## Experiment

We will reproduce the problem using concurrent requests.

### Scenario

Initial inventory:
- Available stock: 10
- Concurrent requests: 50
- Requested quantity per request: 1

### Expected Behavior

At most 10 requests should successfully reserve stock.

### Actual Behavior

To be measured using the concurrency test.

## Solutions To Evaluate

1. Baseline implementation
2. Optimistic locking using JPA @Version
3. Pessimistic database locking
4. Redis-based distributed locking/reservation

Each approach will be tested and compared.

## Metrics

We will record:

- Successful reservations
- Failed reservations
- Overselling occurrences
- Response time
- Throughput
- Locking/contention behavior

## Final Result
 ============================================================
To be filled after experiments.
# Concurrency Experiments

## Experiment 1 — Optimistic Locking

### Setup

- Initial inventory: 10
- Concurrent requests: 50
- Quantity per request: 1
- Locking strategy: JPA Optimistic Locking
- Artificial delay: 100 ms

### Result

- Successful reservations: 5
- Failed reservations: 45
- Final available stock: 5
- Final reserved stock: 5
- Inventory invariant: 10

### Observation

Multiple requests attempted to modify the same inventory row concurrently.

JPA optimistic locking uses the inventory version column to ensure that an update only succeeds when the entity version is still current.

The database update contains:

UPDATE inventory
SET ..., version = ?
WHERE id = ?
AND version = ?

Therefore, stale transactions cannot overwrite a newer inventory state.

### Conclusion

Optimistic locking prevented inconsistent inventory updates and maintained the inventory invariant:

available stock + reserved stock = initial stock.

================================
## Experiment 2 — Pessimistic Locking

### Setup

- Initial inventory: 10
- Concurrent requests: 50
- Quantity per request: 1
- Locking strategy: JPA PESSIMISTIC_WRITE
- Artificial delay: 100 ms

### Result

- Successful reservations: 10
- Failed reservations: 40
- Final available stock: 0
- Final reserved stock: 10
- Inventory invariant: 10

### Observation

Pessimistic locking acquires a database row lock before modifying
the inventory.

Concurrent transactions attempting to modify the same inventory
row must wait for the lock to be released.

As a result, reservations were serialized and exactly the available
10 units were successfully reserved.

### Comparison

Optimistic locking:
- 5 successful reservations
- 45 failed due to concurrent version conflicts

Pessimistic locking:
- 10 successful reservations
- 40 failed after the available inventory was exhausted

### Trade-off

Optimistic locking avoids blocking but may require retries when
contention is high.

Pessimistic locking provides stronger serialization but can cause
transactions to wait for database locks and may reduce throughput
under heavy contention.
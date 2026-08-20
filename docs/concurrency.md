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
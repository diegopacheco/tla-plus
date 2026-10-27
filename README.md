# TLA+

POCs that use TLA+ and the TLC model checker to find concurrency bugs, then fix them in a Java 25 Spring Boot app backed by Postgres.

## What is TLA+

TLA+ (Temporal Logic of Actions) is a formal specification language created by Leslie Lamport.
You describe a system as a set of states and the actions that move it from one state to the next,
then write the properties that must always hold (invariants) or must eventually happen (liveness).
The TLC model checker explores every reachable state and every interleaving of actions, and when a
property breaks it returns the exact step by step trace that leads to the bug.

TLA+ checks the design, not the code. It runs before, or next to, the implementation.

## Why it matters

Concurrency and distributed bugs hide in rare interleavings: two requests reading the same value,
a message arriving after a timeout, a node crashing between two writes. Unit and integration tests
only see the interleavings that happen to run, so these bugs pass review, pass CI and show up in
production as data corruption, double spending or a stuck system. TLC does not sample, it checks
all interleavings of the model, so a bug that needs 7 precise steps is found in seconds.

## Benefits

* Finds design bugs before a single line of code is written, when they are cheapest to fix
* Exhaustive: checks every interleaving within the model bounds, not a lucky sample
* Gives a precise error trace that can be replayed as a deterministic test, as these POCs do
* Forces clear thinking: vague designs cannot be written as a spec
* Small specs: a few dozen lines model a protocol that takes thousands of lines of code
* Safe refactoring and optimization: change the design, rerun TLC, know if it is still correct
* Living documentation of the invariants the system must keep
* Proven in industry: AWS, Microsoft Azure, MongoDB, Elastic and others use it on core systems

## Top 10 use cases

1. Race conditions in shared state: counters, quotas, balances, inventory
2. Money movement: transfers, payments, refunds, double spend and overdraft prevention
3. Distributed consensus and replication: Raft, Paxos, leader election, failover
4. Distributed transactions: two phase commit, sagas, compensation flows
5. Locking and deadlock freedom: lock ordering, lease expiration, distributed locks
6. Cache and database consistency: invalidation, write through, read your writes
7. Message queues and event processing: at least once, exactly once, idempotency, ordering
8. Rate limiting and admission control across many nodes
9. Database and storage engines: isolation levels, MVCC, crash recovery, compaction
10. Workflow and state machines: order lifecycles, retries, timeouts, schedulers

## POCs

* [rate-limit-quotas-tla](rate-limit-quotas-tla/README.md)
  Models API nodes sharing a quota counter, and TLC proves read then write lets them exceed the quota.
  The fix is one atomic conditional SQL update, verified by TLC and by concurrent tests on Postgres.

* [bank-transfer-race-conditions-tla](bank-transfer-race-conditions-tla/README.md)
  Models concurrent money transfers, and TLC finds an overdraft, a lost update and a deadlock.
  The fix locks both accounts in id order inside one transaction, verified by TLC and by stress tests.

## Tooling

* [TLA+ for VS Code](https://marketplace.visualstudio.com/items?itemName=tlaplus.vscode-ide) (`tlaplus.vscode-ide`)
  The official TLA+ extension: syntax highlighting, parsing, and running TLC on a `.tla` spec with its `.cfg` straight from the editor.

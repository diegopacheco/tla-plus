# TLA+

POCs that use TLA+ and the TLC model checker to find concurrency bugs, then fix them in a Java 25 Spring Boot app backed by Postgres.

## POCs

* [rate-limit-quotas-tla](rate-limit-quotas-tla/README.md)
  Models API nodes sharing a quota counter, and TLC proves read then write lets them exceed the quota.
  The fix is one atomic conditional SQL update, verified by TLC and by concurrent tests on Postgres.

* [bank-transfer-race-conditions-tla](bank-transfer-race-conditions-tla/README.md)
  Models concurrent money transfers, and TLC finds an overdraft, a lost update and a deadlock.
  The fix locks both accounts in id order inside one transaction, verified by TLC and by stress tests.

Each POC has its own `scripts/` folder: `setup.sh`, `start-all.sh`, `status.sh`, `tlc.sh`, `test-all.sh`, `ui.sh`, `stop-all.sh` and `sql-console.sh`.

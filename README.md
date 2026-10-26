# TLA+

POCs that use TLA+ and the TLC model checker to find concurrency bugs, then fix them in a Java 25 Spring Boot app backed by Postgres.

| POC | What TLC finds |
|---|---|
| [rate-limit-quotas-tla](rate-limit-quotas-tla/README.md) | Read then write counters let nodes exceed the quota |
| [bank-transfer-race-conditions-tla](bank-transfer-race-conditions-tla/README.md) | Overdraft, lost update and deadlock in money transfers |

Each POC has its own `scripts/` folder: `setup.sh`, `start-all.sh`, `status.sh`, `tlc.sh`, `test-all.sh`, `ui.sh`, `stop-all.sh` and `sql-console.sh`.

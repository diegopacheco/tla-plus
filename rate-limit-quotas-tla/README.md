# Rate Limit Quotas with TLA+

Several API nodes share one quota counter per tenant (fixed window, `Limit` requests per window).
TLA+ proves that a node that reads the counter, decides, then writes it back lets tenants go over
their quota, and proves that one atomic conditional update never does.

## Why it matters

* Quotas are money and safety: paid tiers, abuse protection, downstream capacity. Over granting is a revenue and outage problem
* The bug only shows up under concurrent load across nodes, never in a single threaded unit test or a code review
* TLC finds it in under a second with 3 nodes and a limit of 2, and hands over the exact 7 step trace
* The same trace is replayed as a deterministic JUnit test on Postgres, so the bug is reproducible on demand, not flaky
* The fix is verified twice: TLC checks every interleaving of the model, and a 200 request concurrent test checks the real code
* Window resets are in the model too, so a stale write that lands after a reset is covered

## Model

`spec/RateLimitQuota.tla`

| Config | Atomic | Result |
|---|---|---|
| `RateLimitQuotaRace.cfg` | `FALSE` read then write | TLC finds `QuotaRespected` violated |
| `RateLimitQuotaSafe.cfg` | `TRUE` conditional increment | No error, `QuotaRespected` and `NoLostUpdates` hold |

Error trace found by TLC (Limit = 2):

```
State 1  initial                    used = 0  granted = 0
State 2  n1 reads used = 0          used = 0  granted = 0
State 3  n1 writes 1, grants        used = 1  granted = 1
State 4  n1 reads used = 1          used = 1  granted = 1
State 5  n2 reads used = 1          used = 1  granted = 1
State 6  n1 writes 2, grants        used = 2  granted = 2
State 7  n2 writes 2, grants        used = 2  granted = 3   QuotaRespected violated
```

## Fix

The app uses a single conditional SQL statement, the same as the `Atomic` action in the model:

```sql
UPDATE quota SET used = used + 1 WHERE tenant = :tenant AND used < :limit
```

## Proof

Everything below is real output of `./scripts/test-all.sh` and the running app.
TLC runs inside JUnit, so the model checks and the Java tests run in one suite.

| Test | What it proves |
|---|---|
| `RateLimitQuotaModelCheckTest.tlcFindsThatReadThenWriteNodesExceedTheQuota` | TLC reports `QuotaRespected` violated and the trace ends with `granted = 3` |
| `RateLimitQuotaModelCheckTest.tlcProvesTheAtomicConditionalIncrementNeverExceedsTheQuotaNorLosesUpdates` | TLC explores all 17 distinct states with no error |
| `NaiveQuotaRaceTest.tlcErrorTraceGrantsThreeRequestsWithAQuotaOfTwo` | The TLC trace step by step against Postgres: 3 grants, counter says 2 |
| `AtomicQuotaServiceTest.concurrentNodesNeverGrantMoreThanTheQuota` | 200 concurrent requests on 32 threads grant exactly 10 with a limit of 10 |
| `AtomicQuotaServiceTest.windowResetGivesTheTenantAFreshQuota` | The 11th request is rejected and a new window accepts again |

```
❯ ./scripts/test-all.sh
tests started
postgres up on postgresql://localhost:5432/ratelimit
PASS AtomicQuotaServiceTest.concurrentNodesNeverGrantMoreThanTheQuota
PASS AtomicQuotaServiceTest.windowResetGivesTheTenantAFreshQuota
PASS NaiveQuotaRaceTest.tlcErrorTraceGrantsThreeRequestsWithAQuotaOfTwo
PASS RateLimitQuotaModelCheckTest.tlcProvesTheAtomicConditionalIncrementNeverExceedsTheQuotaNorLosesUpdates
PASS RateLimitQuotaModelCheckTest.tlcFindsThatReadThenWriteNodesExceedTheQuota
total 5, failures 0, errors 0, skipped 0
tests passed, TLC traces in app/target/tlc/
```

The model check tests fail loud: setting `Atomic = FALSE` in `RateLimitQuotaSafe.cfg` turns the safe test into
`FAIL RateLimitQuotaModelCheckTest.tlcProvesTheAtomicConditionalIncrementNeverExceedsTheQuotaNorLosesUpdates` and exit code 1.

Live API with the default limit of 5:

```
POST /api/quotas/acme/consume -> 200
POST /api/quotas/acme/consume -> 200
POST /api/quotas/acme/consume -> 200
POST /api/quotas/acme/consume -> 200
POST /api/quotas/acme/consume -> 200
POST /api/quotas/acme/consume -> 429
GET  /api/quotas/acme         -> {"tenant":"acme","used":5,"limit":5}
```

## App

Java 25, Spring Boot 4.1, Spring Data JDBC, HikariCP, Postgres.

* `AtomicQuotaService` the safe implementation behind the API
* `NaiveQuotaService` the read and write steps of the racy model, used to replay the TLC trace

| Method | Path | Result |
|---|---|---|
| POST | `/api/quotas/{tenant}/consume` | `200` granted, `429` quota exhausted |
| GET | `/api/quotas/{tenant}` | current usage and limit |

## Scripts

All scripts live in `scripts/` and run from any directory of the repository.

| Script | What it does |
|---|---|
| `./scripts/setup.sh` | Downloads tla2tools.jar, pulls Postgres and builds the app |
| `./scripts/start-all.sh` | Starts Postgres and the app and prints the full link of each one |
| `./scripts/status.sh` | Shows every service port as UP or DOWN |
| `./scripts/test-all.sh` | Runs every test: TLC model checks and Java tests, one PASS or FAIL line per test |
| `./scripts/ui.sh` | Opens the quota status of tenant acme in the browser |
| `./scripts/stop-all.sh` | Stops every service |
| `./scripts/sql-console.sh` | Opens a psql console on the database |

Ports are declared in `scripts/ports.env`: app `8080`, postgres `5432`.

```bash
./scripts/setup.sh
./scripts/start-all.sh
./scripts/status.sh
./scripts/test-all.sh
./scripts/ui.sh
./scripts/stop-all.sh
```

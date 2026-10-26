# Rate Limit Quotas with TLA+

Several API nodes share one quota counter per tenant (fixed window, `Limit` requests per window).
The TLA+ model checks if the quota can be exceeded when nodes read the counter, decide, and then write it back.

## Model

`spec/RateLimitQuota.tla`

| Config | Atomic | Result |
|---|---|---|
| `RateLimitQuotaRace.cfg` | `FALSE` read then write | TLC finds `QuotaRespected` violated |
| `RateLimitQuotaSafe.cfg` | `TRUE` conditional increment | No error, `QuotaRespected` and `NoLostUpdates` hold |

Error trace found by TLC (Limit = 2):

```
n2 reads used = 0
n1 reads used = 0
n2 writes used = 1, granted = 1
n1 writes used = 1, granted = 2   lost update, counter says 1
n1 reads used = 1 again, writes 2, granted = 3 > Limit
```

Window reset is modeled too: a node that read before the reset writes a stale value after it.

## Fix

The app uses a single conditional SQL statement, the same as the `Atomic` action in the model:

```sql
UPDATE quota SET used = used + 1 WHERE tenant = :tenant AND used < :limit
```

## App

Java 25, Spring Boot 4.1, Spring Data JDBC, HikariCP, Postgres.

* `AtomicQuotaService` the safe implementation behind the API
* `NaiveQuotaService` the read and write steps of the racy model, used to replay the TLC trace
* `NaiveQuotaRaceTest` replays the TLC error trace against Postgres and proves the quota is exceeded
* `AtomicQuotaServiceTest` 200 concurrent requests grant exactly the quota, and a window reset gives a fresh quota

| Method | Path | Result |
|---|---|---|
| POST | `/api/quotas/{tenant}/consume` | `200` granted, `429` quota exhausted |
| GET | `/api/quotas/{tenant}` | current usage and limit |

```bash
curl -X POST http://localhost:8080/api/quotas/acme/consume
```

## Scripts

All scripts live in `scripts/` and run from any directory of the repository.

| Script | What it does |
|---|---|
| `./scripts/setup.sh` | Downloads tla2tools.jar, pulls Postgres and builds the app |
| `./scripts/start-all.sh` | Starts Postgres and the app and prints the full link of each one |
| `./scripts/status.sh` | Shows every service port as UP or DOWN |
| `./scripts/tlc.sh` | Runs TLC on every model, race models must fail, Safe models must pass |
| `./scripts/test-all.sh` | Runs TLC and then the Java test suite |
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

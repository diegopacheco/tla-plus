# Bank Transfer Race Conditions with TLA+

Concurrent transfers between accounts alice (100) and bob (50), total 150.
Transfers: alice to bob 60, alice to bob 60, bob to alice 30.
Each transfer is split in the steps a real service does: check, debit, read destination, credit.

## Model

`spec/BankTransfer.tla`, the `Mode` constant selects the implementation.

| Config | Mode | Result |
|---|---|---|
| `BankTransferOverdraftRace.cfg` | `unsafe` no locks | `NoOverdraft` violated, alice ends at -20 |
| `BankTransferLostUpdateRace.cfg` | `unsafe` no locks | `MoneyConserved` violated, money is created |
| `BankTransferDeadlock.cfg` | `unordered` lock from then to | Deadlock reached |
| `BankTransferSafe.cfg` | `ordered` lock lowest id first | No error |

Error traces found by TLC:

```
Overdraft (check then act):
  t1 check alice 100 >= 60
  t2 check alice 100 >= 60
  t1 debit alice 40
  t2 debit alice -20

Lost update (read then write):
  t1 debit alice 40, t1 reads bob 50
  t3 debit bob 20
  t1 writes bob 50 + 60 = 110   t3 debit is lost
  t3 credits alice, total 180

Deadlock (lock order):
  t1 locks alice, t3 locks bob
  t1 waits for bob, t3 waits for alice
```

## Fix

Lock both accounts in id order inside one transaction, then check and move the money:

```sql
SELECT * FROM account WHERE id IN (:first, :second) ORDER BY id FOR UPDATE
```

## App

Java 25, Spring Boot 4.1, Spring Data JDBC, HikariCP, Postgres.

* `SafeTransferService` the ordered lock implementation behind the API
* `UnsafeTransferService` the steps of the unsafe model, used to replay the TLC traces
* `UnsafeTransferRaceTest` replays the overdraft and the lost update traces against Postgres
* `SafeTransferServiceTest` runs the same transfers concurrently plus 300 opposite direction transfers, no overdraft, no lost money, no deadlock

| Method | Path | Result |
|---|---|---|
| GET | `/api/accounts` | all accounts |
| POST | `/api/transfers` | `200 COMPLETED` or `409 INSUFFICIENT_FUNDS` |

```bash
curl -X POST -H 'Content-Type: application/json' -d '{"from":1,"to":2,"amount":60}' http://localhost:8082/api/transfers
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
| `./scripts/ui.sh` | Opens the account list in the browser |
| `./scripts/stop-all.sh` | Stops every service |
| `./scripts/sql-console.sh` | Opens a psql console on the database |

Ports are declared in `scripts/ports.env`: app `8082`, postgres `5433`.

```bash
./scripts/setup.sh
./scripts/start-all.sh
./scripts/status.sh
./scripts/test-all.sh
./scripts/ui.sh
./scripts/stop-all.sh
```

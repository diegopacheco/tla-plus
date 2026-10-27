# Bank Transfer Race Conditions with TLA+

Concurrent transfers between accounts alice (100) and bob (50), total 150.
Transfers: t1 alice to bob 60, t2 alice to bob 60, t3 bob to alice 30.
Each transfer is split in the steps a real service does: check, debit, read destination, credit.
TLA+ finds three different production bugs in the obvious implementations and proves the fix has none of them.

## Why it matters

* Money bugs are the worst bugs: an overdraft or money created out of thin air is a financial and regulatory incident
* All three bugs pass code review, pass single threaded tests and only appear under concurrent load
* TLC finds each one in under a second and returns the shortest trace: 3 to 7 steps
* Adding locks is not enough: the naive lock order trades the race for a deadlock, and TLC catches that too
* Each trace is replayed as a deterministic JUnit test on Postgres, so the bug is reproducible on demand
* The fix is verified twice: TLC checks every interleaving of the model, and 300 concurrent opposite direction transfers check the real code

## Model

`spec/BankTransfer.tla`, the `Mode` constant selects the implementation.

| Config | Mode | Result |
|---|---|---|
| `BankTransferOverdraftRace.cfg` | `unsafe` no locks | `NoOverdraft` violated, alice ends at -20 |
| `BankTransferLostUpdateRace.cfg` | `unsafe` no locks | `MoneyConserved` violated, money is created |
| `BankTransferDeadlock.cfg` | `unordered` lock from then to | Deadlock reached |
| `BankTransferSafe.cfg` | `ordered` lock lowest id first | No error |

Error traces found by TLC, balances are `<<alice, bob>>`:

```
Overdraft, check then act:
  State 1  initial                       <<100, 50>>
  State 2  t1 checks alice 100 >= 60     <<100, 50>>
  State 3  t2 checks alice 100 >= 60     <<100, 50>>
  State 4  t1 debits alice               <<40, 50>>
  State 5  t2 debits alice               <<-20, 50>>   NoOverdraft violated

Lost update, read then write:
  State 1  initial                       <<100, 50>>
  State 2  t1 checks alice               <<100, 50>>
  State 3  t1 debits alice               <<40, 50>>
  State 4  t1 reads bob = 50             <<40, 50>>
  State 5  t3 checks bob                 <<40, 50>>
  State 6  t3 debits bob                 <<40, 20>>
  State 7  t1 writes bob 50 + 60         <<40, 110>>   MoneyConserved violated, t3 debit is lost
                                                       40 + 110 + 30 in flight = 180

Deadlock, lock source first:
  State 1  initial                       lock = <<free, free>>
  State 2  t1 locks alice                lock = <<t1, free>>
  State 3  t3 locks bob                  lock = <<t1, t3>>     t1 waits bob, t3 waits alice
```

## Fix

Lock both accounts in id order inside one transaction, then check and move the money:

```sql
SELECT * FROM account WHERE id IN (:first, :second) ORDER BY id FOR UPDATE
```

## Proof

Everything below is real output of `./scripts/test-all.sh` and the running app.
TLC runs inside JUnit, so the model checks and the Java tests run in one suite.

| Test | What it proves |
|---|---|
| `BankTransferModelCheckTest.tlcFindsTheCheckThenActOverdraftWithoutLocks` | TLC reports `NoOverdraft` violated, trace ends with `balance = <<-20, 50>>` |
| `BankTransferModelCheckTest.tlcFindsTheLostUpdateThatCreatesMoneyWithoutLocks` | TLC reports `MoneyConserved` violated, trace ends with `balance = <<40, 110>>` |
| `BankTransferModelCheckTest.tlcFindsTheDeadlockWhenEachTransferLocksItsSourceFirst` | TLC reports a deadlock, trace ends with `lock = <<1, 3>>` |
| `BankTransferModelCheckTest.tlcProvesOrderedLocksNeverOverdraftLoseMoneyNorDeadlock` | TLC explores all 100 distinct states with no error and no deadlock |
| `UnsafeTransferRaceTest.tlcOverdraftTraceCheckCheckDebitDebitLeavesAliceNegative` | The overdraft trace against Postgres: alice ends at -20 |
| `UnsafeTransferRaceTest.tlcLostUpdateTraceCreatesMoneyOutOfThinAir` | The lost update trace against Postgres, then t3 settles: total 180 instead of 150 |
| `SafeTransferServiceTest.sameTransfersAsTheTlcModelNeverOverdraftNorLoseMoney` | t1, t2, t3 concurrently: balances match the completed transfers exactly, total 150 |
| `SafeTransferServiceTest.oppositeDirectionStressNeverDeadlocksOverdraftsOrLosesMoney` | 300 concurrent transfers in both directions finish, no negative balance, total 150 |

```
❯ ./scripts/test-all.sh
tests started
postgres up on postgresql://localhost:5433/bank
PASS BankTransferModelCheckTest.tlcFindsTheLostUpdateThatCreatesMoneyWithoutLocks
PASS BankTransferModelCheckTest.tlcProvesOrderedLocksNeverOverdraftLoseMoneyNorDeadlock
PASS BankTransferModelCheckTest.tlcFindsTheCheckThenActOverdraftWithoutLocks
PASS BankTransferModelCheckTest.tlcFindsTheDeadlockWhenEachTransferLocksItsSourceFirst
PASS SafeTransferServiceTest.sameTransfersAsTheTlcModelNeverOverdraftNorLoseMoney
PASS SafeTransferServiceTest.oppositeDirectionStressNeverDeadlocksOverdraftsOrLosesMoney
PASS UnsafeTransferRaceTest.tlcLostUpdateTraceCreatesMoneyOutOfThinAir
PASS UnsafeTransferRaceTest.tlcOverdraftTraceCheckCheckDebitDebitLeavesAliceNegative
total 8, failures 0, errors 0, skipped 0
tests passed, TLC traces in app/target/tlc/
```

Live API, the second 60 transfer is rejected and the total stays 150:

```
GET  /api/accounts                     -> [{"id":1,"owner":"alice","balance":100},{"id":2,"owner":"bob","balance":50}]
POST /api/transfers {1 -> 2, 60}       -> "COMPLETED" 200
POST /api/transfers {1 -> 2, 60}       -> "INSUFFICIENT_FUNDS" 409
GET  /api/accounts                     -> [{"id":1,"owner":"alice","balance":40},{"id":2,"owner":"bob","balance":110}]
```

## App

Java 25, Spring Boot 4.1, Spring Data JDBC, HikariCP, Postgres.

* `SafeTransferService` the ordered lock implementation behind the API
* `UnsafeTransferService` the steps of the unsafe model, used to replay the TLC traces

| Method | Path | Result |
|---|---|---|
| GET | `/api/accounts` | all accounts |
| POST | `/api/transfers` | `200 COMPLETED` or `409 INSUFFICIENT_FUNDS` |

## Scripts

All scripts live in `scripts/` and run from any directory of the repository.

| Script | What it does |
|---|---|
| `./scripts/setup.sh` | Downloads tla2tools.jar, pulls Postgres and builds the app |
| `./scripts/start-all.sh` | Starts Postgres and the app and prints the full link of each one |
| `./scripts/status.sh` | Shows every service port as UP or DOWN |
| `./scripts/test-all.sh` | Runs every test: TLC model checks and Java tests, one PASS or FAIL line per test |
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

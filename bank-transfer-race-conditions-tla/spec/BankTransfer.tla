---- MODULE BankTransfer ----
EXTENDS Naturals, Integers, FiniteSets

CONSTANTS Mode

Accounts == {1, 2}

InitialBalance == [a \in Accounts |-> IF a = 1 THEN 100 ELSE 50]

Transfers == <<[from |-> 1, to |-> 2, amount |-> 60],
               [from |-> 1, to |-> 2, amount |-> 60],
               [from |-> 2, to |-> 1, amount |-> 30]>>

Ids == DOMAIN Transfers

Total == 150

VARIABLES balance, pc, localTo, lock

vars == <<balance, pc, localTo, lock>>

Min(a, b) == IF a < b THEN a ELSE b
Max(a, b) == IF a < b THEN b ELSE a

FirstLock(t) ==
    IF Mode = "ordered"
    THEN Min(Transfers[t].from, Transfers[t].to)
    ELSE Transfers[t].from

SecondLock(t) ==
    IF Mode = "ordered"
    THEN Max(Transfers[t].from, Transfers[t].to)
    ELSE Transfers[t].to

Free == 0

TypeOK ==
    /\ Mode \in {"unsafe", "unordered", "ordered"}
    /\ balance \in [Accounts -> Int]
    /\ pc \in [Ids -> {"lock1", "lock2", "check", "debit", "readTo", "credit", "unlock", "done"}]
    /\ localTo \in [Ids -> Int]
    /\ lock \in [Accounts -> Ids \cup {Free}]

Init ==
    /\ balance = InitialBalance
    /\ pc = [t \in Ids |-> IF Mode = "unsafe" THEN "check" ELSE "lock1"]
    /\ localTo = [t \in Ids |-> 0]
    /\ lock = [a \in Accounts |-> Free]

Goto(t, label) == pc' = [pc EXCEPT ![t] = label]

AcquireFirst(t) ==
    /\ pc[t] = "lock1"
    /\ lock[FirstLock(t)] = Free
    /\ lock' = [lock EXCEPT ![FirstLock(t)] = t]
    /\ Goto(t, "lock2")
    /\ UNCHANGED <<balance, localTo>>

AcquireSecond(t) ==
    /\ pc[t] = "lock2"
    /\ lock[SecondLock(t)] = Free
    /\ lock' = [lock EXCEPT ![SecondLock(t)] = t]
    /\ Goto(t, "check")
    /\ UNCHANGED <<balance, localTo>>

Check(t) ==
    /\ pc[t] = "check"
    /\ IF balance[Transfers[t].from] >= Transfers[t].amount
       THEN Goto(t, "debit")
       ELSE Goto(t, IF Mode = "unsafe" THEN "done" ELSE "unlock")
    /\ UNCHANGED <<balance, localTo, lock>>

Debit(t) ==
    /\ pc[t] = "debit"
    /\ balance' = [balance EXCEPT ![Transfers[t].from] = @ - Transfers[t].amount]
    /\ Goto(t, "readTo")
    /\ UNCHANGED <<localTo, lock>>

ReadTo(t) ==
    /\ pc[t] = "readTo"
    /\ localTo' = [localTo EXCEPT ![t] = balance[Transfers[t].to]]
    /\ Goto(t, "credit")
    /\ UNCHANGED <<balance, lock>>

Credit(t) ==
    /\ pc[t] = "credit"
    /\ balance' = [balance EXCEPT ![Transfers[t].to] = localTo[t] + Transfers[t].amount]
    /\ Goto(t, IF Mode = "unsafe" THEN "done" ELSE "unlock")
    /\ UNCHANGED <<localTo, lock>>

Release(t) ==
    /\ pc[t] = "unlock"
    /\ lock' = [a \in Accounts |-> IF lock[a] = t THEN Free ELSE lock[a]]
    /\ Goto(t, "done")
    /\ UNCHANGED <<balance, localTo>>

Finished ==
    /\ \A t \in Ids : pc[t] = "done"
    /\ UNCHANGED vars

Next ==
    \/ \E t \in Ids :
        \/ AcquireFirst(t)
        \/ AcquireSecond(t)
        \/ Check(t)
        \/ Debit(t)
        \/ ReadTo(t)
        \/ Credit(t)
        \/ Release(t)
    \/ Finished

Spec == Init /\ [][Next]_vars

RECURSIVE SumBalances(_)
SumBalances(S) ==
    IF S = {} THEN 0
    ELSE LET a == CHOOSE x \in S : TRUE IN balance[a] + SumBalances(S \ {a})

RECURSIVE SumInFlight(_)
SumInFlight(S) ==
    IF S = {} THEN 0
    ELSE LET t == CHOOSE x \in S : TRUE IN
         (IF pc[t] \in {"readTo", "credit"} THEN Transfers[t].amount ELSE 0) + SumInFlight(S \ {t})

NoOverdraft == \A a \in Accounts : balance[a] >= 0

MoneyConserved == SumBalances(Accounts) + SumInFlight(Ids) = Total

====

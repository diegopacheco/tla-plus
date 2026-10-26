---- MODULE RateLimitQuota ----
EXTENDS Naturals

CONSTANTS Nodes, Limit, MaxRequests, MaxWindows, Atomic

VARIABLES used, granted, pc, seen, requests, window

vars == <<used, granted, pc, seen, requests, window>>

TypeOK ==
    /\ used \in Nat
    /\ granted \in Nat
    /\ pc \in [Nodes -> {"idle", "read"}]
    /\ seen \in [Nodes -> Nat]
    /\ requests \in 0..MaxRequests
    /\ window \in 0..MaxWindows

Init ==
    /\ used = 0
    /\ granted = 0
    /\ pc = [n \in Nodes |-> "idle"]
    /\ seen = [n \in Nodes |-> 0]
    /\ requests = 0
    /\ window = 0

Grant(count) ==
    IF count < Limit
    THEN /\ used' = count + 1
         /\ granted' = granted + 1
    ELSE UNCHANGED <<used, granted>>

ArriveAtomic(n) ==
    /\ Atomic
    /\ pc[n] = "idle"
    /\ requests < MaxRequests
    /\ requests' = requests + 1
    /\ Grant(used)
    /\ UNCHANGED <<pc, seen, window>>

ArriveRead(n) ==
    /\ ~Atomic
    /\ pc[n] = "idle"
    /\ requests < MaxRequests
    /\ requests' = requests + 1
    /\ seen' = [seen EXCEPT ![n] = used]
    /\ pc' = [pc EXCEPT ![n] = "read"]
    /\ UNCHANGED <<used, granted, window>>

CommitWrite(n) ==
    /\ pc[n] = "read"
    /\ pc' = [pc EXCEPT ![n] = "idle"]
    /\ Grant(seen[n])
    /\ UNCHANGED <<seen, requests, window>>

ResetWindow ==
    /\ window < MaxWindows
    /\ window' = window + 1
    /\ used' = 0
    /\ granted' = 0
    /\ UNCHANGED <<pc, seen, requests>>

Finished ==
    /\ requests = MaxRequests
    /\ \A n \in Nodes : pc[n] = "idle"
    /\ UNCHANGED vars

Next ==
    \/ \E n \in Nodes : ArriveAtomic(n) \/ ArriveRead(n) \/ CommitWrite(n)
    \/ ResetWindow
    \/ Finished

Spec == Init /\ [][Next]_vars

QuotaRespected == granted <= Limit

NoLostUpdates == used = granted

====

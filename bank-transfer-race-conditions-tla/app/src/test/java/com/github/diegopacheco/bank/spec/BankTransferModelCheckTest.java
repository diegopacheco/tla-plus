package com.github.diegopacheco.bank.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BankTransferModelCheckTest {

    @Test
    void tlcFindsTheCheckThenActOverdraftWithoutLocks() throws Exception {
        TlcResult result = Tlc.check("BankTransfer", "BankTransferOverdraftRace");

        assertTrue(result.violated("NoOverdraft"), result.describe());
        assertTrue(result.lastState().contains("/\\ balance = <<-20, 50>>"),
                "alice ends at -20 after two 60 transfers both passed the check, " + result.describe());
    }

    @Test
    void tlcFindsTheLostUpdateThatCreatesMoneyWithoutLocks() throws Exception {
        TlcResult result = Tlc.check("BankTransfer", "BankTransferLostUpdateRace");

        assertTrue(result.violated("MoneyConserved"), result.describe());
        assertTrue(result.lastState().contains("/\\ balance = <<40, 110>>"),
                "bob's debit of 30 was overwritten while 30 is still in flight to alice, " + result.describe());
    }

    @Test
    void tlcFindsTheDeadlockWhenEachTransferLocksItsSourceFirst() throws Exception {
        TlcResult result = Tlc.check("BankTransfer", "BankTransferDeadlock");

        assertTrue(result.deadlockReached(), result.describe());
        assertTrue(result.lastState().contains("/\\ lock = <<1, 3>>"),
                "transfer 1 holds alice and transfer 3 holds bob, each waits for the other, " + result.describe());
    }

    @Test
    void tlcProvesOrderedLocksNeverOverdraftLoseMoneyNorDeadlock() throws Exception {
        TlcResult result = Tlc.check("BankTransfer", "BankTransferSafe");

        assertTrue(result.noErrorFound(), result.describe());
    }
}

package com.github.diegopacheco.bank.transfer;

import com.github.diegopacheco.bank.account.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UnsafeTransferRaceTest {

    @Autowired
    private UnsafeTransferService unsafe;

    @Autowired
    private AccountRepository accounts;

    @Test
    void tlcOverdraftTraceCheckCheckDebitDebitLeavesAliceNegative() {
        TestAccounts ids = TestAccounts.open(accounts);
        Transfer t1 = new Transfer(ids.alice(), ids.bob(), 60);
        Transfer t2 = new Transfer(ids.alice(), ids.bob(), 60);

        assertTrue(unsafe.check(t1));
        assertTrue(unsafe.check(t2), "both checks see 100 because neither debit happened yet");
        unsafe.debit(t1);
        unsafe.debit(t2);

        assertEquals(-20, accounts.findBalance(ids.alice()), "NoOverdraft is violated exactly like TLC reported");
    }

    @Test
    void tlcLostUpdateTraceCreatesMoneyOutOfThinAir() {
        TestAccounts ids = TestAccounts.open(accounts);
        Transfer t1 = new Transfer(ids.alice(), ids.bob(), 60);
        Transfer t3 = new Transfer(ids.bob(), ids.alice(), 30);

        assertTrue(unsafe.check(t1));
        unsafe.debit(t1);
        long bobSeenByT1 = unsafe.readTo(t1);
        assertTrue(unsafe.check(t3));
        unsafe.debit(t3);
        unsafe.credit(t1, bobSeenByT1);
        long aliceSeenByT3 = unsafe.readTo(t3);
        unsafe.credit(t3, aliceSeenByT3);

        assertEquals(70, accounts.findBalance(ids.alice()));
        assertEquals(110, accounts.findBalance(ids.bob()), "bob kept the 30 he sent because t1 overwrote the debit");
        assertEquals(180, ids.total(accounts), "MoneyConserved is violated: 150 became 180");
    }
}

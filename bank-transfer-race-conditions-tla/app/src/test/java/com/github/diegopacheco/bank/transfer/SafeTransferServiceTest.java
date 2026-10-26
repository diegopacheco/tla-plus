package com.github.diegopacheco.bank.transfer;

import com.github.diegopacheco.bank.account.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SafeTransferServiceTest {

    @Autowired
    private SafeTransferService service;

    @Autowired
    private AccountRepository accounts;

    @Test
    void sameTransfersAsTheTlcModelNeverOverdraftNorLoseMoney() throws Exception {
        TestAccounts ids = TestAccounts.open(accounts);
        List<Transfer> transfers = List.of(
                new Transfer(ids.alice(), ids.bob(), 60),
                new Transfer(ids.alice(), ids.bob(), 60),
                new Transfer(ids.bob(), ids.alice(), 30));

        List<TransferResult> results = runConcurrently(transfers);

        long expectedAlice = 100;
        for (int i = 0; i < transfers.size(); i++) {
            if (results.get(i) == TransferResult.COMPLETED) {
                Transfer t = transfers.get(i);
                expectedAlice += t.to() == ids.alice() ? t.amount() : -t.amount();
            }
        }
        assertEquals(expectedAlice, accounts.findBalance(ids.alice()),
                "alice's balance reflects every completed transfer exactly once, no lost updates");
        assertTrue(accounts.findBalance(ids.alice()) >= 0);
        assertTrue(accounts.findBalance(ids.bob()) >= 0);
        assertEquals(150, ids.total(accounts));
    }

    @Test
    void oppositeDirectionStressNeverDeadlocksOverdraftsOrLosesMoney() throws Exception {
        TestAccounts ids = TestAccounts.open(accounts);
        List<Transfer> transfers = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            transfers.add(i % 2 == 0
                    ? new Transfer(ids.alice(), ids.bob(), 1 + i % 40)
                    : new Transfer(ids.bob(), ids.alice(), 1 + i % 40));
        }

        runConcurrently(transfers);

        assertTrue(accounts.findBalance(ids.alice()) >= 0);
        assertTrue(accounts.findBalance(ids.bob()) >= 0);
        assertEquals(150, ids.total(accounts));
    }

    private List<TransferResult> runConcurrently(List<Transfer> transfers) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TransferResult>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
            for (Transfer transfer : transfers) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return service.transfer(transfer);
                }));
            }
            start.countDown();
            List<TransferResult> results = new ArrayList<>();
            for (Future<TransferResult> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        }
    }
}

package com.github.diegopacheco.bank.transfer;

import com.github.diegopacheco.bank.account.AccountRepository;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.concurrent.ThreadLocalRandom;

record TestAccounts(long alice, long bob) {

    static TestAccounts open(AccountRepository accounts) {
        long alice = ThreadLocalRandom.current().nextLong(1_000, Long.MAX_VALUE / 2);
        long bob = alice + 1;
        accounts.open(alice, "alice", 100);
        accounts.open(bob, "bob", 50);
        return new TestAccounts(alice, bob);
    }

    void close(JdbcClient jdbc) {
        jdbc.sql("DELETE FROM account WHERE id IN (:alice, :bob)")
                .param("alice", alice)
                .param("bob", bob)
                .update();
    }

    long total(AccountRepository accounts) {
        return accounts.findBalance(alice) + accounts.findBalance(bob);
    }
}

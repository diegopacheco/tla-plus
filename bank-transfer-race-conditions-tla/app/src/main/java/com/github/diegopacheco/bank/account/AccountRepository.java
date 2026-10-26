package com.github.diegopacheco.bank.account;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AccountRepository extends Repository<Account, Long> {

    @Query("SELECT * FROM account ORDER BY id")
    List<Account> findAll();

    @Query("SELECT * FROM account WHERE id IN (:first, :second) ORDER BY id FOR UPDATE")
    List<Account> lockInIdOrder(long first, long second);

    @Query("SELECT balance FROM account WHERE id = :id")
    long findBalance(long id);

    @Modifying
    @Query("UPDATE account SET balance = balance - :amount WHERE id = :id")
    void withdraw(long id, long amount);

    @Modifying
    @Query("UPDATE account SET balance = balance + :amount WHERE id = :id")
    void deposit(long id, long amount);

    @Modifying
    @Query("UPDATE account SET balance = :balance WHERE id = :id")
    void writeBalance(long id, long balance);

    @Modifying
    @Query("INSERT INTO account (id, owner, balance) VALUES (:id, :owner, :balance) ON CONFLICT (id) DO UPDATE SET owner = EXCLUDED.owner, balance = EXCLUDED.balance")
    void open(long id, String owner, long balance);
}

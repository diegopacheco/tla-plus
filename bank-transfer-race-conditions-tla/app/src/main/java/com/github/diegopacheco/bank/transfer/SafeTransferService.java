package com.github.diegopacheco.bank.transfer;

import com.github.diegopacheco.bank.account.Account;
import com.github.diegopacheco.bank.account.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SafeTransferService {

    private final AccountRepository accounts;

    public SafeTransferService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public TransferResult transfer(Transfer transfer) {
        List<Account> locked = accounts.lockInIdOrder(transfer.from(), transfer.to());
        if (locked.size() != 2) {
            throw new NoSuchElementException("both accounts must exist");
        }
        Account from = locked.get(0).id() == transfer.from() ? locked.get(0) : locked.get(1);
        if (from.balance() < transfer.amount()) {
            return TransferResult.INSUFFICIENT_FUNDS;
        }
        accounts.withdraw(transfer.from(), transfer.amount());
        accounts.deposit(transfer.to(), transfer.amount());
        return TransferResult.COMPLETED;
    }
}

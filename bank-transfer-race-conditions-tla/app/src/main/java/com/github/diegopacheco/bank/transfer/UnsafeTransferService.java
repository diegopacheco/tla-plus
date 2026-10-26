package com.github.diegopacheco.bank.transfer;

import com.github.diegopacheco.bank.account.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class UnsafeTransferService {

    private final AccountRepository accounts;

    public UnsafeTransferService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public boolean check(Transfer transfer) {
        return accounts.findBalance(transfer.from()) >= transfer.amount();
    }

    public void debit(Transfer transfer) {
        accounts.withdraw(transfer.from(), transfer.amount());
    }

    public long readTo(Transfer transfer) {
        return accounts.findBalance(transfer.to());
    }

    public void credit(Transfer transfer, long localTo) {
        accounts.writeBalance(transfer.to(), localTo + transfer.amount());
    }
}

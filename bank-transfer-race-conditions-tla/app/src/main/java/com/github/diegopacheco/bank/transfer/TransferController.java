package com.github.diegopacheco.bank.transfer;

import com.github.diegopacheco.bank.account.Account;
import com.github.diegopacheco.bank.account.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TransferController {

    private final SafeTransferService transfers;
    private final AccountRepository accounts;

    public TransferController(SafeTransferService transfers, AccountRepository accounts) {
        this.transfers = transfers;
        this.accounts = accounts;
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResult> transfer(@RequestBody Transfer transfer) {
        TransferResult result = transfers.transfer(transfer);
        HttpStatus status = result == TransferResult.COMPLETED ? HttpStatus.OK : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(result);
    }

    @GetMapping("/accounts")
    public List<Account> accounts() {
        return accounts.findAll();
    }
}

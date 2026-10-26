package com.github.diegopacheco.bank.transfer;

public record Transfer(long from, long to, long amount) {

    public Transfer {
        if (from == to) {
            throw new IllegalArgumentException("from and to must be different accounts");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}

package com.github.diegopacheco.bank.account;

import org.springframework.data.annotation.Id;

public record Account(@Id long id, String owner, long balance) {
}

package com.github.diegopacheco.ratelimit.quota;

import org.springframework.data.annotation.Id;

public record Quota(@Id String tenant, int used) {
}

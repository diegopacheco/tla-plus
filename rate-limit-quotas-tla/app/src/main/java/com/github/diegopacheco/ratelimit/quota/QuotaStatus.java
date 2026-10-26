package com.github.diegopacheco.ratelimit.quota;

public record QuotaStatus(String tenant, int used, int limit) {
}

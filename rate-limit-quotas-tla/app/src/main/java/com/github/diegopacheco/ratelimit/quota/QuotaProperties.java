package com.github.diegopacheco.ratelimit.quota;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("quota")
public record QuotaProperties(int limit, long windowMs) {
}

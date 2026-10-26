package com.github.diegopacheco.ratelimit.quota;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AtomicQuotaService {

    private final QuotaRepository repository;
    private final QuotaProperties properties;

    public AtomicQuotaService(QuotaRepository repository, QuotaProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public boolean consume(String tenant) {
        repository.ensureExists(tenant);
        return repository.incrementIfBelow(tenant, properties.limit()) == 1;
    }

    public QuotaStatus status(String tenant) {
        repository.ensureExists(tenant);
        return new QuotaStatus(tenant, repository.findUsed(tenant), properties.limit());
    }

    @Scheduled(fixedRateString = "${quota.window-ms}", initialDelayString = "${quota.window-ms}")
    public void resetWindow() {
        repository.resetAll();
    }
}

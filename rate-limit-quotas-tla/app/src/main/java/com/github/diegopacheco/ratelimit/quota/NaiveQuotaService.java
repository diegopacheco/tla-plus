package com.github.diegopacheco.ratelimit.quota;

import org.springframework.stereotype.Service;

@Service
public class NaiveQuotaService {

    private final QuotaRepository repository;
    private final QuotaProperties properties;

    public NaiveQuotaService(QuotaRepository repository, QuotaProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public int read(String tenant) {
        repository.ensureExists(tenant);
        return repository.findUsed(tenant);
    }

    public boolean commit(String tenant, int seen) {
        if (seen >= properties.limit()) {
            return false;
        }
        repository.writeUsed(tenant, seen + 1);
        return true;
    }
}

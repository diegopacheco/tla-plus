package com.github.diegopacheco.ratelimit.quota;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

public interface QuotaRepository extends Repository<Quota, String> {

    @Modifying
    @Query("INSERT INTO quota (tenant, used) VALUES (:tenant, 0) ON CONFLICT (tenant) DO NOTHING")
    void ensureExists(String tenant);

    @Modifying
    @Query("UPDATE quota SET used = used + 1 WHERE tenant = :tenant AND used < :limit")
    int incrementIfBelow(String tenant, int limit);

    @Query("SELECT used FROM quota WHERE tenant = :tenant")
    int findUsed(String tenant);

    @Modifying
    @Query("UPDATE quota SET used = :used WHERE tenant = :tenant")
    void writeUsed(String tenant, int used);

    @Modifying
    @Query("UPDATE quota SET used = 0")
    void resetAll();
}

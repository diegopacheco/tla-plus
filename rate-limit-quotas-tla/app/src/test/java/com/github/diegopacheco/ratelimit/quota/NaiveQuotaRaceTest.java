package com.github.diegopacheco.ratelimit.quota;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"quota.limit=2", "quota.window-ms=3600000"})
class NaiveQuotaRaceTest {

    @Autowired
    private NaiveQuotaService naive;

    @Autowired
    private JdbcClient jdbc;

    private final String tenant = UUID.randomUUID().toString();

    @AfterEach
    void deleteTenant() {
        jdbc.sql("DELETE FROM quota WHERE tenant = :tenant").param("tenant", tenant).update();
    }

    @Test
    void tlcErrorTraceGrantsThreeRequestsWithAQuotaOfTwo() {
        int granted = 0;

        int firstSeenByN1 = naive.read(tenant);
        granted += naive.commit(tenant, firstSeenByN1) ? 1 : 0;
        int secondSeenByN1 = naive.read(tenant);
        int seenByN2 = naive.read(tenant);
        granted += naive.commit(tenant, secondSeenByN1) ? 1 : 0;
        granted += naive.commit(tenant, seenByN2) ? 1 : 0;

        assertEquals(1, secondSeenByN1);
        assertEquals(1, seenByN2, "n2 decided on the same stale value n1 was about to overwrite");
        assertEquals(3, granted);
        assertTrue(granted > 2, "QuotaRespected is violated exactly like TLC reported");
        assertEquals(2, naive.read(tenant), "the counter says 2 while 3 requests were granted, one increment was lost");
    }
}

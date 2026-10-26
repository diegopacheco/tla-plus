package com.github.diegopacheco.ratelimit.quota;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"quota.limit=2", "quota.window-ms=3600000"})
class NaiveQuotaRaceTest {

    @Autowired
    private NaiveQuotaService naive;

    @Test
    void tlcErrorTraceReadReadReadCommitCommitCommitGrantsMoreThanTheQuota() {
        String tenant = UUID.randomUUID().toString();

        int seenByNode1 = naive.read(tenant);
        int seenByNode2 = naive.read(tenant);
        int seenByNode3 = naive.read(tenant);

        int granted = 0;
        granted += naive.commit(tenant, seenByNode1) ? 1 : 0;
        granted += naive.commit(tenant, seenByNode2) ? 1 : 0;
        granted += naive.commit(tenant, seenByNode3) ? 1 : 0;

        assertEquals(3, granted);
        assertTrue(granted > 2, "the quota of 2 was exceeded because every node decided on a stale read");
        assertEquals(1, naive.read(tenant), "the counter lost two of the three increments");
    }
}

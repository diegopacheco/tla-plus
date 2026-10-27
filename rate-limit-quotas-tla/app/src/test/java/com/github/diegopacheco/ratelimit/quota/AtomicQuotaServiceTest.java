package com.github.diegopacheco.ratelimit.quota;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"quota.limit=10", "quota.window-ms=3600000"})
class AtomicQuotaServiceTest {

    @Autowired
    private AtomicQuotaService service;

    @Autowired
    private JdbcClient jdbc;

    private final String tenant = UUID.randomUUID().toString();

    @AfterEach
    void deleteTenant() {
        jdbc.sql("DELETE FROM quota WHERE tenant = :tenant").param("tenant", tenant).update();
    }

    @Test
    void concurrentNodesNeverGrantMoreThanTheQuota() throws Exception {
        int requests = 200;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(32)) {
            for (int i = 0; i < requests; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return service.consume(tenant);
                }));
            }
            start.countDown();
            int granted = 0;
            for (Future<Boolean> result : results) {
                granted += result.get() ? 1 : 0;
            }
            assertEquals(10, granted, "exactly the quota is granted, never more, never less");
        }
        assertEquals(10, service.status(tenant).used(), "the counter matches the grants, no lost updates");
    }

    @Test
    void windowResetGivesTheTenantAFreshQuota() {
        for (int i = 0; i < 10; i++) {
            assertTrue(service.consume(tenant));
        }
        assertFalse(service.consume(tenant), "the 11th request in the same window is rejected");

        service.resetWindow();

        assertTrue(service.consume(tenant), "a new window accepts requests again");
        assertEquals(1, service.status(tenant).used());
    }
}

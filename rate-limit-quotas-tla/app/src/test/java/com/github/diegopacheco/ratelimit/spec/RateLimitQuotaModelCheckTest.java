package com.github.diegopacheco.ratelimit.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitQuotaModelCheckTest {

    @Test
    void tlcFindsThatReadThenWriteNodesExceedTheQuota() throws Exception {
        TlcResult result = Tlc.check("RateLimitQuota", "RateLimitQuotaRace");

        assertTrue(result.violated("QuotaRespected"), result.describe());
        assertTrue(result.lastState().contains("/\\ granted = 3"),
                "three grants with Limit = 2 in the final state of the trace, " + result.describe());
    }

    @Test
    void tlcProvesTheAtomicConditionalIncrementNeverExceedsTheQuotaNorLosesUpdates() throws Exception {
        TlcResult result = Tlc.check("RateLimitQuota", "RateLimitQuotaSafe");

        assertTrue(result.noErrorFound(), result.describe());
    }
}

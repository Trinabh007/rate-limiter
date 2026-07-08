package com.RateMesh.ratemesh;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.RateMesh.ratemesh.TokenBucket.TokenBucket;

public class TokenBucketTest {
    
    @Test
    void shouldAllowRequestsWithinCapacity() {
    TokenBucket tokenBucket = new TokenBucket(5, 1);
    for (int i = 0; i < 5; i++) {
        assertTrue(tokenBucket.tryAcquire());
    }
    assertFalse(tokenBucket.tryAcquire());
    }
    @Test
    void shouldDenyRequestWhenCapacityExceeded() {
        TokenBucket tokenBucket = new TokenBucket(1, 1);
        assertTrue(tokenBucket.tryAcquire());
        assertFalse(tokenBucket.tryAcquire());
    }
    @Test
    void shouldRefillTokensOverTime() throws InterruptedException {
        TokenBucket tokenBucket = new TokenBucket(1, 1);
        tokenBucket.tryAcquire(); // Consume the only token
        Thread.sleep(1500); // Wait for more than 1 second to allow refill
        assertTrue(tokenBucket.tryAcquire()); // Should be able to acquire again 
        assertFalse(tokenBucket.tryAcquire());

    }
}

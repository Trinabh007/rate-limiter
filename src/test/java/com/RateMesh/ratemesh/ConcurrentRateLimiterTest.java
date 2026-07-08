package com.RateMesh.ratemesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.RateMesh.ratemesh.TokenBucket.TokenBucket;

public  class ConcurrentRateLimiterTest {
    @Test
    void shouldNotExceedLimitUnderConcurrentLoad() throws InterruptedException {

    TokenBucket bucket = new TokenBucket(10, 1.0);
    int threadCount = 20;
    AtomicInteger allowed = new AtomicInteger(0);
    AtomicInteger denied = new AtomicInteger(0);

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            if (bucket.tryAcquire()) {
                allowed.incrementAndGet();
            } else {
                denied.incrementAndGet();
            }
        });
    }

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);

    System.out.println("Allowed: " + allowed.get());
    System.out.println("Denied: " + denied.get());

    assertTrue(allowed.get() <= 10);
    assertEquals(20, allowed.get() + denied.get());
    }
}

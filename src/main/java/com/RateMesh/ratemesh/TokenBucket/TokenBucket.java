package com.RateMesh.ratemesh.TokenBucket;

import com.RateMesh.ratemesh.RateLimiter.RateLimiter;

public class TokenBucket implements RateLimiter {
    
    private final long capacity; // Max bucket capacity
    private final double rate; // Tokens per second
    private double tokens; // Current tokens
    private long lastRefillTime; // Last refill timestamp (ms)
    public TokenBucket(long capacity, double rate) {
        this.capacity = capacity;
        this.rate = rate;
        this.tokens = capacity; // Bucket starts full
        this.lastRefillTime = System.currentTimeMillis();
    }
    @Override
    public boolean allowRequest(String clientId) {
    return tryAcquire();
    }
    public synchronized boolean tryAcquire() {
        // 1. Refill tokens
        refill();
        // 2. Attempt to acquire 1 token
        if (tokens >= 1) {
            tokens--;
            return true; // Success
        }
        return false; // Rate limited
    }
    private void refill() {
        long now = System.currentTimeMillis();
        double elapsedTime = (now - lastRefillTime) / 1000.0; // Seconds
        double newTokens = elapsedTime * rate;
        if (newTokens > 0) {
            tokens = Math.min(tokens + newTokens, capacity); // Cap at max
            lastRefillTime = now;
        }
    }
}

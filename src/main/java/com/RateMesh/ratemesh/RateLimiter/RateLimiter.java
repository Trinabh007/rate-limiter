package com.RateMesh.ratemesh.RateLimiter;

public interface RateLimiter {
    boolean tryAcquire();
}

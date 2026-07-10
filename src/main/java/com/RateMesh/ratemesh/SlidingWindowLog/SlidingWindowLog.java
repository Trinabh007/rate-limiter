package com.RateMesh.ratemesh.SlidingWindowLog;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import com.RateMesh.ratemesh.RateLimiter.RateLimiter;

public class SlidingWindowLog implements RateLimiter {

    private final int capacity;
    private final long windowSizeInMillis;
    private final ConcurrentHashMap<String, Deque<Long>> clientWindows;

    public SlidingWindowLog(int capacity, long windowSizeInMillis) {
        this.capacity = capacity;
        this.windowSizeInMillis = windowSizeInMillis;
        this.clientWindows = new ConcurrentHashMap<>();
    }

    public synchronized boolean allowRequest(String clientId) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - windowSizeInMillis;

        Deque<Long> window = clientWindows.computeIfAbsent(clientId, k -> new ArrayDeque<>());

        // Step 1: evict expired timestamps from the front
        while (window.peekFirst() != null && window.peekFirst() < windowStart) {
            window.pollFirst();
        }

        // Step 2: check if limit is reached
        if (window.size() >= capacity) {
            return false;
        }

        // Step 3: record this request and allow
        window.addLast(currentTime);
        return true;
    }    
}
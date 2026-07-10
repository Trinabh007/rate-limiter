package com.RateMesh.ratemesh;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.RateMesh.ratemesh.SlidingWindowLog.SlidingWindowLog;

public class SlidingWindowLogTest {
    
    @Test
    void shouldAllowRequestsWithinLimit() {
        SlidingWindowLog slidingWindowLog = new SlidingWindowLog(5, 1000); // 5 requests per second
        for (int i = 0; i < 5; i++) {
            assertTrue(slidingWindowLog.allowRequest("client1")); // Should allow the first 5 requests
        }
        assertFalse(slidingWindowLog.allowRequest("client1")); // Should deny the 6th request
    }
    @Test
    void shouldDenyRequestWhenCapacityExceeded() {
        SlidingWindowLog slidingWindowLog = new SlidingWindowLog(1, 1000); // 1 request per second
        assertTrue(slidingWindowLog.allowRequest("client2")); // Should allow the first request
        assertFalse(slidingWindowLog.allowRequest("client2")); // Should deny the 2nd request
    }
    @Test
    void shouldAllowRequestsAfterWindowExpires() throws InterruptedException {
        SlidingWindowLog slidingWindowLog = new SlidingWindowLog(1, 1000);
        slidingWindowLog.allowRequest("client3"); // Consume the only request
        Thread.sleep(1500); // Wait for more than 1 second to allow the window to expire
        assertTrue(slidingWindowLog.allowRequest("client3")); // Should be able to acquire again
        assertFalse(slidingWindowLog.allowRequest("client3")); // Should deny the 2nd request
    }
}

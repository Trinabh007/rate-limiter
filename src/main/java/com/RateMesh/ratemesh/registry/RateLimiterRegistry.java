/*
 * KNOWN LIMITATION: In-Memory State Does Not Scale Horizontally
 *
 * Each instance of RateMesh maintains its own RateLimiterRegistry in JVM memory.
 * When multiple instances run behind a load balancer, each instance has an
 * independent copy of every client's rate limiter state.
 *
 * Example with limit = 3 requests and 2 instances:
 * - Instance A receives 3 requests from client1 → allows all 3 (sees 3/3)
 * - Instance B receives 3 requests from client1 → allows all 3 (sees 3/3)
 * - Actual requests served = 6, double the intended limit
 *
 * Root cause: synchronized only prevents race conditions within a single JVM.
 * It has no effect across separate processes.
 *
 * Fix (Week 2): Move rate limiter state to Redis. Both instances share one
 * Redis instance. State is updated atomically using Lua scripts.
 */
package com.RateMesh.ratemesh.registry;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.RateMesh.ratemesh.SlidingWindowLog.SlidingWindowLog;
import com.RateMesh.ratemesh.TokenBucket.TokenBucket;
import com.RateMesh.ratemesh.config.ClientConfig;

@Component
public class RateLimiterRegistry {
    
    private final ConcurrentHashMap<String,ClientConfig> clientConfigMap;
    private final ConcurrentHashMap<String,Object> rateLimiterMap;
    public RateLimiterRegistry() {
        clientConfigMap = new ConcurrentHashMap<>();
        rateLimiterMap = new ConcurrentHashMap<>();
    }
    public void registerClientConfig(String clientId, ClientConfig clientConfig) {
        clientConfigMap.put(clientId, clientConfig);
        String algoString = clientConfig.getAlgorithm();
        if(algoString.equals("TOKEN_BUCKET")) {
            rateLimiterMap.put(clientId, new TokenBucket(clientConfig.getMaxRequests(), clientConfig.getRefillRate()));
        }
        else if(algoString.equals("SLIDING_WINDOW_LOG")) {
            rateLimiterMap.put(clientId,new SlidingWindowLog(clientConfig.getMaxRequests(), clientConfig.getWindowSizeInMillis())); 
        }
    }
    public ClientConfig getClientConfig(String clientId) {
        return clientConfigMap.get(clientId);
    }
    public Object getRateLimiter(String clientId) {
        return rateLimiterMap.get(clientId);
    }
}

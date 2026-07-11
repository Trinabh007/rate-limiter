/*
 * Client config is stored in two places:
 * - JVM ConcurrentHashMap: local cache for fast reads (nanosecond lookup)
 * - Redis Hash: source of truth, shared across all instances (survives restarts)
 *
 * On registerClientConfig: write to both local map and Redis.
 * On getRateLimiter: check local map first. On miss, hydrate from Redis.
 * On Redis miss: return null → interceptor returns 404.
 *
 * This pattern is read-heavy, write-rarely — ideal for local caching.
 */
package com.RateMesh.ratemesh.registry;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.RateMesh.ratemesh.RateLimiter.RateLimiter;
import com.RateMesh.ratemesh.SlidingWindowLog.RedisSlidingWindowLog;
import com.RateMesh.ratemesh.TokenBucket.RedisTokenBucket;
import com.RateMesh.ratemesh.config.ClientConfig;
import com.RateMesh.ratemesh.constants.AppConstants;

@Component
public class RateLimiterRegistry {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterRegistry.class);
    private static final String CONFIG_KEY_PREFIX = "client-config:";

    private final ConcurrentHashMap<String, ClientConfig> clientConfigMap;
    private final ConcurrentHashMap<String, RateLimiter> rateLimiterMap;
    private final RedisTemplate<String, String> redisTemplate;
    private final ConcurrentHashMap<String, Long> sentinelTimestamps = new ConcurrentHashMap<>();
    private static final long SENTINEL_TTL_MS = 60_000L;
    public RateLimiterRegistry(RedisTemplate<String, String> redisTemplate) {
        this.clientConfigMap = new ConcurrentHashMap<>();
        this.rateLimiterMap = new ConcurrentHashMap<>();
        this.redisTemplate = redisTemplate;
    }

    public void registerClientConfig(String clientId, ClientConfig clientConfig) {
        // 1. Write to local maps
        clientConfigMap.put(clientId, clientConfig);
        buildAndStoreRateLimiter(clientId, clientConfig);
        sentinelTimestamps.remove(clientId);
        // 2. Write to Redis — source of truth for all instances
        redisTemplate.opsForHash().putAll(CONFIG_KEY_PREFIX + clientId, Map.of(
                "algorithm", clientConfig.getAlgorithm(),
                "maxRequests", String.valueOf(clientConfig.getMaxRequests()),
                "windowSizeInMillis", String.valueOf(clientConfig.getWindowSizeInMillis()),
                "refillRate", String.valueOf(clientConfig.getRefillRate())
        ));
        redisTemplate.expire(CONFIG_KEY_PREFIX + clientId, Duration.ofDays(7));
        log.info("Registered client '{}' with algorithm {} — written to local cache and Redis",
                clientId, clientConfig.getAlgorithm());
        redisTemplate.convertAndSend("config-invalidated", AppConstants.INSTANCE_ID + ":" + clientId);
    }
    private static final ClientConfig UNKNOWN_CONFIG = new ClientConfig(
    "__UNKNOWN__", 0, 0L, 0.0
    );
    public ClientConfig getClientConfig(String clientId) {
        ClientConfig local = clientConfigMap.get(clientId);
        if(local == UNKNOWN_CONFIG){
            Long storedAt = sentinelTimestamps.get(clientId);
        if (storedAt != null && System.currentTimeMillis() - storedAt > SENTINEL_TTL_MS) {
            // expired — evict, fall through to re-check Redis
            clientConfigMap.remove(clientId);
            sentinelTimestamps.remove(clientId);
        } else {
            // still valid — return null immediately, no Redis call
            return null;
        }
        }
        if (local != null) return local;
        
        // Miss — try to hydrate from Redis
        clientConfigMap.computeIfAbsent(clientId, id -> {
    hydrateFromRedis(id);
    ClientConfig hydrated = clientConfigMap.get(id);
    return hydrated != null ? hydrated : UNKNOWN_CONFIG;
    });

    ClientConfig result = clientConfigMap.get(clientId);
    if (result == UNKNOWN_CONFIG) {
    sentinelTimestamps.put(clientId, System.currentTimeMillis());
    return null;
    }
    return result;
}
    private static final RateLimiter UNKNOWN_CLIENT = new RateLimiter() {
    @Override
    public boolean allowRequest(String clientId) {
        throw new UnsupportedOperationException("Unknown client");
    }
    };

    public RateLimiter getRateLimiter(String clientId) {
    RateLimiter current = rateLimiterMap.get(clientId);

    // sentinel present — check if expired
    if (current == UNKNOWN_CLIENT) {
        Long storedAt = sentinelTimestamps.get(clientId);
        if (storedAt != null && System.currentTimeMillis() - storedAt > SENTINEL_TTL_MS) {
            // expired — evict, fall through to re-check Redis
            rateLimiterMap.remove(clientId);
            sentinelTimestamps.remove(clientId);
        } else {
            // still valid — return null immediately, no Redis call
            return null;
        }
    }

    // key absent (or just evicted) — hydrate from Redis
    if (current == null || !rateLimiterMap.containsKey(clientId)) {
        rateLimiterMap.computeIfAbsent(clientId, id -> {
            hydrateFromRedis(id);
            RateLimiter hydrated = rateLimiterMap.get(id);
            return hydrated != null ? hydrated : UNKNOWN_CLIENT;
        });
        // store timestamp if sentinel was just placed
        RateLimiter after = rateLimiterMap.get(clientId);
        if (after == UNKNOWN_CLIENT) {
            sentinelTimestamps.put(clientId, System.currentTimeMillis());
        }
    }

    RateLimiter result = rateLimiterMap.get(clientId);
    return result == UNKNOWN_CLIENT ? null : result;
}

    // Fetches client config from Redis and populates both local maps.
    // No-op if the key doesn't exist in Redis.
    private void hydrateFromRedis(String clientId) {
        Map<Object, Object> fields = redisTemplate.opsForHash()
                .entries(CONFIG_KEY_PREFIX + clientId);

        if (fields == null || fields.isEmpty()) {
            log.warn("No config found in Redis for client '{}'", clientId);
            return;
        }

        String algorithm = (String) fields.get("algorithm");
        int maxRequests = Integer.parseInt((String) fields.get("maxRequests"));
        long windowSizeInMillis = Long.parseLong((String) fields.get("windowSizeInMillis"));
        double refillRate = Double.parseDouble((String) fields.get("refillRate"));

        ClientConfig config = new ClientConfig(algorithm, maxRequests, windowSizeInMillis, refillRate);
        clientConfigMap.put(clientId, config);
        buildAndStoreRateLimiter(clientId, config);

        log.info("Hydrated client '{}' from Redis into local cache", clientId);
    }

    // Constructs the correct RateLimiter implementation and puts it in rateLimiterMap.
    private void buildAndStoreRateLimiter(String clientId, ClientConfig config) {
        String algorithm = config.getAlgorithm();
        if (algorithm.equals("TOKEN_BUCKET")) {
            try {
                rateLimiterMap.put(clientId, new RedisTokenBucket(
                        redisTemplate,
                        clientId,
                        config.getMaxRequests(),
                        config.getRefillRate()
                ));
            } catch (IOException e) {
                throw new RuntimeException("Failed to load token_bucket.lua", e);
            }
        } else if (algorithm.equals("SLIDING_WINDOW_LOG")) {
            rateLimiterMap.put(clientId, new RedisSlidingWindowLog(
                    redisTemplate,
                    clientId,
                    config.getMaxRequests(),
                    config.getWindowSizeInMillis()
            ));
        } else {
            throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        }
    }
    public void evictLocalCache(String clientId) {
    clientConfigMap.remove(clientId);
    rateLimiterMap.remove(clientId);
    sentinelTimestamps.remove(clientId);
    log.info("Evicted local cache for client '{}'", clientId);
}
}
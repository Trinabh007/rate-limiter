package com.RateMesh.ratemesh.TokenBucket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.RateMesh.ratemesh.RateLimiter.RateLimiter;

public class RedisTokenBucket implements RateLimiter {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> script;
    private final String clientId;
    private final int capacity;
    private final double refillRate;
    public RedisTokenBucket(RedisTemplate<String, String> redisTemplate,String clientId, int capacity, double refillRate) throws IOException {
        this.redisTemplate = redisTemplate;
        this.clientId = clientId;
        this.capacity = capacity;
        this.refillRate = refillRate;
        String scriptText = new ClassPathResource("token_bucket.lua")
                .getContentAsString(StandardCharsets.UTF_8);
        this.script = RedisScript.of(scriptText, Long.class);
    }
    @Override
    public boolean tryAcquire() {
        String tokensKey = "ratemesh:" + clientId + ":tokens";
        String refillKey = "ratemesh:" + clientId + ":lastRefill";

        Long result = redisTemplate.execute(
                script,
                List.of(tokensKey, refillKey),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(System.currentTimeMillis())
        );

        return result != null && result == 1L;
    }
}

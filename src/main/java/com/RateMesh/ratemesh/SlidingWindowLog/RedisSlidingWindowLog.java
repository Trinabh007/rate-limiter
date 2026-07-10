package com.RateMesh.ratemesh.SlidingWindowLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.RateMesh.ratemesh.RateLimiter.RateLimiter;

public class RedisSlidingWindowLog implements RateLimiter{
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> script;
    private final String clientId;
    private final int capacity;
    private final long windowSizeInMillis;
    public RedisSlidingWindowLog(RedisTemplate<String, String> redisTemplate,String clientId, int capacity, long windowSizeInMillis) {
        this.redisTemplate = redisTemplate;
        this.clientId = clientId;
        this.capacity = capacity;
        this.windowSizeInMillis = windowSizeInMillis;
         try {
            String scriptText = new ClassPathResource("sliding_windowlog.lua")
                    .getContentAsString(StandardCharsets.UTF_8);
            this.script = RedisScript.of(scriptText, Long.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Lua script", e);
        }
    }
    @Override
    public boolean allowRequest(String clientId) {
        return tryAcquire();
    }
    public boolean tryAcquire() {
        String windowKey = "ratemesh:" + this.clientId + ":window";
        String currentTime = String.valueOf(System.currentTimeMillis());
        Long result = redisTemplate.execute(
                script,
                List.of(windowKey),
                currentTime,
                String.valueOf(windowSizeInMillis),
                String.valueOf(capacity)
        );
        return result != null && result == 1L;
    }
}

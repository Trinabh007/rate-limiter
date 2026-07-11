package com.RateMesh.ratemesh.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.RateMesh.ratemesh.constants.AppConstants;
import com.RateMesh.ratemesh.registry.RateLimiterRegistry;

@Component
public class ConfigurationInvalidatorListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationInvalidatorListener.class);

    private final RateLimiterRegistry rateLimiterRegistry;

    public ConfigurationInvalidatorListener(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        String[] parts = body.split(":", 2);
        String senderId = parts[0];
        String clientId = parts[1];

        if (senderId.equals(AppConstants.INSTANCE_ID)) {
            log.debug("Skipping self-invalidation for client '{}'", clientId);
            return;
        }

        log.info("Invalidating local cache for client '{}'", clientId);
        rateLimiterRegistry.evictLocalCache(clientId);
    }
}
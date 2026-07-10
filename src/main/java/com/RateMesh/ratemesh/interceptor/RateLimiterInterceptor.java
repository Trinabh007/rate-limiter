package com.RateMesh.ratemesh.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.RateMesh.ratemesh.RateLimiter.RateLimiter;
import com.RateMesh.ratemesh.SlidingWindowLog.SlidingWindowLog;
import com.RateMesh.ratemesh.config.ClientConfig;
import com.RateMesh.ratemesh.registry.RateLimiterRegistry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component
public class RateLimiterInterceptor implements HandlerInterceptor {
    private final RateLimiterRegistry rateLimiterRegistry;
    public RateLimiterInterceptor(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String clientId = request.getHeader("X-Client-Id");
        if (clientId == null || clientId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing X-Client-Id header");
            return false;
        }
        ClientConfig clientConfig = rateLimiterRegistry.getClientConfig(clientId);
        if (clientConfig == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Client not registered");
            return false;
        }
        Object rateLimiter = rateLimiterRegistry.getRateLimiter(clientId);
        boolean allowed = false;
        if (clientConfig.getAlgorithm().equals("TOKEN_BUCKET")) {
            allowed = ((RateLimiter)rateLimiter).tryAcquire();
        } else if (clientConfig.getAlgorithm().equals("SLIDING_WINDOW_LOG")) {
            allowed = ((SlidingWindowLog)rateLimiter).tryAcquire(clientId);
        }
        if(!allowed) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(clientConfig.getWindowSizeInMillis() / 1000));
            response.getWriter().write("Rate limit exceeded");
            return false;
        }
        return true;
    }
}
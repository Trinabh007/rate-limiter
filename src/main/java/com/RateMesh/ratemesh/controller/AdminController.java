package com.RateMesh.ratemesh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.RateMesh.ratemesh.config.ClientConfig;
import com.RateMesh.ratemesh.config.ClientConfigRequest;
import com.RateMesh.ratemesh.registry.RateLimiterRegistry;
@RestController
@RequestMapping("/admin")
public class AdminController {
    
    private final RateLimiterRegistry rateLimiterRegistry;
    public AdminController(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }
    @PostMapping("/config")
    public ResponseEntity<String> registerClientConfig(@RequestBody ClientConfigRequest clientConfigRequest){
        ClientConfig clientConfig = new ClientConfig(clientConfigRequest.getAlgorithm(), clientConfigRequest.getMaxRequests(), clientConfigRequest.getWindowSizeInMillis(), clientConfigRequest.getRefillRate());
        rateLimiterRegistry.registerClientConfig(clientConfigRequest.getClientId(), clientConfig);
        return ResponseEntity.ok("Client configuration registered successfully.");
    }
}

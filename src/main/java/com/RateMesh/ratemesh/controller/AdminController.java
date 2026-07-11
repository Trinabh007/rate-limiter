package com.RateMesh.ratemesh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.RateMesh.ratemesh.config.ClientConfig;
import com.RateMesh.ratemesh.config.ClientConfigRequest;
import com.RateMesh.ratemesh.registry.RateLimiterRegistry;

import jakarta.servlet.http.HttpServletResponse;
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
    @DeleteMapping("/config/{clientId}")
    public ResponseEntity<String> deregisterClientConfig(@PathVariable String clientId) {
    boolean removed = rateLimiterRegistry.deregisterClientConfig(clientId);
        if (!removed) {
            return ResponseEntity.status(HttpServletResponse.SC_NOT_FOUND)
                .body("Client not found: " + clientId);
        }
    return ResponseEntity.ok("Client configuration deregistered successfully.");
    }
}

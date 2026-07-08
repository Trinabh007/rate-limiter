package com.RateMesh.ratemesh.config;

public class ClientConfig {
    private final String algorithm;
    private final int maxRequests;
    private final long windowSizeInMillis;
    private final double refillRate;

    public ClientConfig(String algorithm, int maxRequests, long windowSizeInMillis, double refillRate) {
        this.algorithm = algorithm;
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInMillis;
        this.refillRate = refillRate;
    }
    public String getAlgorithm() {
        return algorithm;
    }
    public int getMaxRequests() {
        return maxRequests;
    }
    public long getWindowSizeInMillis() {
        return windowSizeInMillis;
    } 
    public double getRefillRate() {
        return refillRate;
    }
}  

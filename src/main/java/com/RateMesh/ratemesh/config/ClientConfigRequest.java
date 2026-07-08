package com.RateMesh.ratemesh.config;

public class ClientConfigRequest {
    private String algorithm;
    private int maxRequests;
    private long windowSizeInMillis;
    private double refillRate;
    private String clientId;
    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public long getWindowSizeInMillis() {
        return windowSizeInMillis;
    }

    public void setWindowSizeInMillis(long windowSizeInMillis) {
        this.windowSizeInMillis = windowSizeInMillis;
    }

    public double getRefillRate() {
        return refillRate;
    }

    public void setRefillRate(double refillRate) {
        this.refillRate = refillRate;
    }
    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}

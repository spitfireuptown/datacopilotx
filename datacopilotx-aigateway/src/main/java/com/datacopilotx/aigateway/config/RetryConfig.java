package com.datacopilotx.aigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.gateway.retry")
public class RetryConfig {

    private int maxAttempts = 3;

    private long minBackoff = 1000;

    private long maxBackoff = 5000;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getMinBackoff() {
        return minBackoff;
    }

    public void setMinBackoff(long minBackoff) {
        this.minBackoff = minBackoff;
    }

    public long getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(long maxBackoff) {
        this.maxBackoff = maxBackoff;
    }
}
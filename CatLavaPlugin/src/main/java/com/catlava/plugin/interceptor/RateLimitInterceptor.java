package com.catlava.plugin.interceptor;

import com.catlava.plugin.config.PluginConfig;
import dev.arbjerg.lavalink.api.RestInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
s
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interceptor for rate limiting requests per IP address.
 * Implements a token bucket algorithm.
 */
@Component
public class RateLimitInterceptor implements RestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    
    private final PluginConfig config;
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStart = new ConcurrentHashMap<>();

    public RateLimitInterceptor(PluginConfig config) {
        this.config = config;
    }

    @NotNull
    @Override
    public Map<String, String> onRequest(@NotNull Map<String, String> headers, @NotNull String path, @NotNull String method) {
        if (!config.getRateLimit().isEnabled()) {
            return headers;
        }

        String clientIp = getClientIp(headers);
        String key = clientIp + ":" + path;

        if (!canMakeRequest(key)) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }

        // Add rate limit headers
        Map<String, String> modifiedHeaders = new HashMap<>(headers);
        RateLimitBucket bucket = buckets.get(key);
        if (bucket != null) {
            modifiedHeaders.put("X-RateLimit-Limit", String.valueOf(config.getRateLimit().getRequestsPerMinute()));
            modifiedHeaders.put("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            modifiedHeaders.put("X-RateLimit-Reset", String.valueOf(bucket.getResetTime() / 1000));
        }

        return modifiedHeaders;
    }

    private boolean canMakeRequest(String key) {
        long now = System.currentTimeMillis();
        RateLimitBucket bucket = buckets.get(key);

        if (bucket == null || bucket.isExpired(now)) {
            // Create new bucket
            int requestsPerMinute = config.getRateLimit().getRequestsPerMinute();
            int burst = config.getRateLimit().getBurst();
            bucket = new RateLimitBucket(requestsPerMinute + burst, now + 60000);
            buckets.put(key, bucket);
            return true;
        }

        return bucket.tryConsume();
    }

    private String getClientIp(Map<String, String> headers) {
        String forwarded = headers.get("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = headers.get("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        return "unknown";
    }

    @Override
    public void onResponse(@NotNull String path, @NotNull String method, int statusCode, @NotNull String body, long timeTakenMs) {
        // Cleanup old buckets periodically
        if (Math.random() < 0.01) { // 1% chance
            cleanupBuckets();
        }
    }

    @Override
    public void onError(@NotNull String path, @NotNull String method, @NotNull Throwable error, long timeTakenMs) {
        log.debug("Rate limit error on {} {}: {}", method, path, error.getMessage());
    }

    private void cleanupBuckets() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    /**
     * Rate limit bucket using token bucket algorithm
     */
    private static class RateLimitBucket {
        private final int maxTokens;
        private final long resetTime;
        private final AtomicInteger availableTokens;

        public RateLimitBucket(int maxTokens, long resetTime) {
            this.maxTokens = maxTokens;
            this.resetTime = resetTime;
            this.availableTokens = new AtomicInteger(maxTokens);
        }

        public boolean tryConsume() {
            while (true) {
                int current = availableTokens.get();
                if (current <= 0) {
                    return false;
                }
                if (availableTokens.compareAndSet(current, current - 1)) {
                    return true;
                }
            }
        }

        public int getAvailableTokens() {
            return availableTokens.get();
        }

        public long getResetTime() {
            return resetTime;
        }

        public boolean isExpired(long now) {
            return now >= resetTime;
        }
    }

    /**
     * Custom exception for rate limit exceeded
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}


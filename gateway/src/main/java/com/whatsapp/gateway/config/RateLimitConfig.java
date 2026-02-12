package com.whatsapp.gateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Configuration using Token Bucket Algorithm
 *
 * Implements rate limiting to prevent API abuse and ensure fair usage.
 * Uses Bucket4j library with Token Bucket algorithm.
 *
 * Rate Limits:
 * - Anonymous users: 10 requests/minute
 * - Authenticated users: 100 requests/minute
 * - Premium users: 500 requests/minute
 * - WebSocket messages: 50 messages/minute per connection
 *
 * Algorithm: Token Bucket
 * - Tokens are added to bucket at a fixed rate (refill rate)
 * - Each request consumes 1 token
 * - If no tokens available, request is rejected (HTTP 429)
 * - Bucket has maximum capacity (burst limit)
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Configuration
public class RateLimitConfig {

    /**
     * In-memory bucket storage per user/IP
     *
     * For production: Consider using Redis for distributed rate limiting
     * across multiple gateway instances.
     */
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    /**
     * Get or create rate limit bucket for a user/IP
     *
     * @param key Unique identifier (userId or IP address)
     * @param bandwidth Rate limit bandwidth configuration
     * @return Bucket for the given key
     */
    public Bucket resolveBucket(String key, Bandwidth bandwidth) {
        return bucketCache.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(bandwidth)
                .build());
    }

    /**
     * Anonymous user rate limit: 10 requests per minute
     * Used for unauthenticated endpoints (login, register)
     */
    @Bean
    public Bandwidth anonymousUserBandwidth() {
        log.info("Configuring Anonymous User Rate Limit: 10 req/min");
        return Bandwidth.classic(
                10,  // capacity
                Refill.intervally(10, Duration.ofMinutes(1))
        );
    }

    /**
     * Authenticated user rate limit: 100 requests per minute
     * Standard rate limit for logged-in users
     */
    @Bean
    public Bandwidth authenticatedUserBandwidth() {
        log.info("Configuring Authenticated User Rate Limit: 100 req/min");
        return Bandwidth.classic(
                100,  // capacity (burst limit)
                Refill.intervally(100, Duration.ofMinutes(1))
        );
    }

    /**
     * Premium user rate limit: 500 requests per minute
     * Higher limit for premium/paid users
     */
    @Bean
    public Bandwidth premiumUserBandwidth() {
        log.info("Configuring Premium User Rate Limit: 500 req/min");
        return Bandwidth.classic(
                500,  // capacity
                Refill.intervally(500, Duration.ofMinutes(1))
        );
    }

    /**
     * WebSocket message rate limit: 50 messages per minute
     * Prevents message spam through WebSocket connections
     */
    @Bean
    public Bandwidth webSocketMessageBandwidth() {
        log.info("Configuring WebSocket Message Rate Limit: 50 msg/min");
        return Bandwidth.classic(
                50,  // capacity
                Refill.intervally(50, Duration.ofMinutes(1))
        );
    }

    /**
     * IP-based rate limit: 200 requests per minute per IP
     * Prevents abuse from single IP address
     */
    @Bean
    public Bandwidth ipBasedBandwidth() {
        log.info("Configuring IP-Based Rate Limit: 200 req/min");
        return Bandwidth.classic(
                200,  // capacity
                Refill.intervally(200, Duration.ofMinutes(1))
        );
    }

    /**
     * Rate limit configuration class
     * Can be externalized to application.yml
     */
    public static class RateLimitProperties {

        // Anonymous users
        public static final int ANONYMOUS_CAPACITY = 10;
        public static final Duration ANONYMOUS_REFILL_PERIOD = Duration.ofMinutes(1);

        // Authenticated users
        public static final int AUTHENTICATED_CAPACITY = 100;
        public static final Duration AUTHENTICATED_REFILL_PERIOD = Duration.ofMinutes(1);

        // Premium users
        public static final int PREMIUM_CAPACITY = 500;
        public static final Duration PREMIUM_REFILL_PERIOD = Duration.ofMinutes(1);

        // WebSocket messages
        public static final int WEBSOCKET_CAPACITY = 50;
        public static final Duration WEBSOCKET_REFILL_PERIOD = Duration.ofMinutes(1);

        // IP-based limiting
        public static final int IP_CAPACITY = 200;
        public static final Duration IP_REFILL_PERIOD = Duration.ofMinutes(1);
    }

    /**
     * Clear bucket cache periodically to prevent memory leaks
     * This should be called by a scheduled job
     */
    public void clearInactiveBuckets() {
        log.info("Clearing rate limit bucket cache. Current size: {}", bucketCache.size());
        bucketCache.clear();
        log.info("Rate limit bucket cache cleared");
    }
}
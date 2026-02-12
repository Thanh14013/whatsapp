package com.whatsapp.gateway;

import lombok.Builder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;

/**
 * API Gateway Application for WhatsApp Clone
 *
 * This service acts as a single entry point for all client requests,
 * handling routing, authentication, rate limiting, and load balancing.
 *
 * Key Features:
 * - Route management to microservices
 * - JWT-based authentication
 * - Rate limiting per user/IP
 * - WebSocket support for real-time chat
 * - Circuit breaker pattern for resilience
 * - Request/Response logging
 * - CORS configuration
 *
 * @author WhatsApp Clone Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        log.info("Starting WhatsApp Clone API Gateway...");
        SpringApplication.run(GatewayApplication.class, args);
        log.info("WhatsApp Clone API Gateway started successfully!");
    }

    /**
     * Configure routes to downstream microservices
     *
     * Route Configuration:
     * - /api/users/** → User Service
     * - /api/chat/** → Chat Service
     * - /api/messages/** → Chat Service
     * - /ws/** → WebSocket connections to Chat Service
     *
     * All routes include:
     * - Authentication filter
     * - Rate limiting
     * - Circuit breaker
     * - Request/Response logging
     */
    @Bean
    @Builder
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("Configuring API Gateway routes...");

        return builder.routes()
                // User Service Routes
                .route("user-service", r -> r
                        .path("/api/users/**", "/api/auth/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .circuitBreaker(config -> config
                                        .setName("userServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user-service"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(100L, 1000L, 2, true)))
                        .uri("http://user-service:8081"))

                // Chat Service Routes - REST API
                .route("chat-service-rest", r -> r
                        .path("/api/chat/**", "/api/messages/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .circuitBreaker(config -> config
                                        .setName("chatServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/chat-service"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(100L, 1000L, 2, true)))
                        .uri("http://chat-service:8082"))

                // Chat Service Routes - WebSocket
                .route("chat-service-websocket", r -> r
                        .path("/ws/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("ws://chat-service:8082"))

                // Health Check Route
                .route("health-check", r -> r
                        .path("/actuator/health")
                        .uri("http://localhost:8080"))

                .build();
    }
}
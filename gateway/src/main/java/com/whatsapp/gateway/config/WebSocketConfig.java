package com.whatsapp.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket Configuration for API Gateway
 *
 * Configures WebSocket support for real-time chat messaging.
 * The gateway proxies WebSocket connections to the Chat Service.
 *
 * Key Features:
 * - WebSocket handshake support
 * - Token-based authentication for WebSocket connections
 * - Connection upgrade from HTTP to WebSocket protocol
 * - Proper routing of WebSocket frames to Chat Service
 *
 * WebSocket Flow:
 * 1. Client sends HTTP upgrade request to /ws/chat
 * 2. Gateway validates JWT token from query parameter or header
 * 3. If valid, upgrade connection to WebSocket protocol
 * 4. Route WebSocket frames to Chat Service
 * 5. Maintain bidirectional communication
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Configuration
public class WebSocketConfig {

    /**
     * Configure WebSocket Handler Adapter
     *
     * This adapter handles the upgrade from HTTP to WebSocket protocol
     * using Reactor Netty as the underlying server.
     */
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        log.info("Configuring WebSocket Handler Adapter...");

        return new WebSocketHandlerAdapter(
                new ReactorNettyRequestUpgradeStrategy()
        );
    }

    /**
     * WebSocket connection properties
     *
     * These can be externalized to application.yml for different environments
     */
    public static class WebSocketProperties {

        /**
         * Maximum WebSocket frame payload length (1 MB)
         * Prevents memory issues from large messages
         */
        public static final int MAX_FRAME_PAYLOAD_LENGTH = 1024 * 1024; // 1 MB

        /**
         * WebSocket idle timeout (5 minutes)
         * Connection closed if no message received within this time
         */
        public static final long IDLE_TIMEOUT_MILLIS = 5 * 60 * 1000; // 5 minutes

        /**
         * WebSocket ping interval (30 seconds)
         * Send ping frames to keep connection alive
         */
        public static final long PING_INTERVAL_MILLIS = 30 * 1000; // 30 seconds

        /**
         * Maximum number of WebSocket connections per user
         * Allows multiple devices per user
         */
        public static final int MAX_CONNECTIONS_PER_USER = 5;

        /**
         * WebSocket buffer size for incoming messages
         */
        public static final int BUFFER_SIZE_BYTES = 8192; // 8 KB
    }
}
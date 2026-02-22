package com.whatsapp.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.extern.slf4j.Slf4j;

/**
 * Chat Service Application
 *
 * Handles real-time messaging and conversations.
 *
 * Key Features:
 * - Real-time messaging via WebSocket
 * - Message persistence (MongoDB)
 * - Conversation management (PostgreSQL)
 * - Message delivery tracking
 * - Undelivered message handling
 * - Message queue integration
 *
 * Architecture: Domain-Driven Design (DDD)
 * - Domain Layer: Business logic (Message, Conversation)
 * - Application Layer: Use cases orchestration
 * - Infrastructure Layer: WebSocket, PostgreSQL, MongoDB, Redis, RabbitMQ
 * - Interface Layer: WebSocket handlers, REST APIs
 *
 * Dual Database Strategy:
 * - PostgreSQL: Conversation metadata, delivery status
 * - MongoDB: Message content (scalable for high volume)
 *
 * @author WhatsApp Clone Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableJpaAuditing
@EnableMongoAuditing
@EnableCaching
@EnableAsync
public class ChatServiceApplication {

    public static void main(String[] args) {
        log.info("Starting WhatsApp Clone Chat Service...");
        SpringApplication.run(ChatServiceApplication.class, args);
        log.info("WhatsApp Clone Chat Service started successfully!");
    }
}
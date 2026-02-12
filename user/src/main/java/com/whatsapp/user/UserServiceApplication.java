package com.whatsapp.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.extern.slf4j.Slf4j;

/**
 * User Service Application
 *
 * Handles user management, authentication, and user profiles.
 *
 * Key Features:
 * - User registration and authentication
 * - JWT token generation and validation
 * - User profile management
 * - User status tracking (online/offline)
 * - Password management
 * - User search and discovery
 *
 * Architecture: Domain-Driven Design (DDD)
 * - Domain Layer: Business logic and rules
 * - Application Layer: Use cases and orchestration
 * - Infrastructure Layer: Database, cache, messaging
 * - Interface Layer: REST APIs and controllers
 *
 * @author WhatsApp Clone Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
public class UserServiceApplication {

    public static void main(String[] args) {
        log.info("Starting WhatsApp Clone User Service...");
        SpringApplication.run(UserServiceApplication.class, args);
        log.info("WhatsApp Clone User Service started successfully!");
    }
}
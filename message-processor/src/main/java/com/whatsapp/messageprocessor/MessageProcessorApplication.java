package com.whatsapp.messageprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/**
 * Message Processor Service Application
 *
 * Background service that consumes messages from RabbitMQ and processes them.
 *
 * Key Responsibilities:
 * - Consume messages from RabbitMQ queues
 * - Process message delivery to offline users
 * - Update message status in database
 * - Clear cache when messages are delivered
 * - Send push notifications for offline users
 * - Handle message retry logic
 *
 * Architecture: Event-Driven
 * - Listens to: message.sent, message.delivered, message.read
 * - Updates: MongoDB (message status), Redis (cache)
 * - Sends: Push notifications via third-party service
 *
 * Message Flow:
 * 1. Message sent → Published to RabbitMQ
 * 2. Consumer picks up message
 * 3. Check if receiver is online (Redis)
 * 4. If offline: Cache in inbox, send push notification
 * 5. If online: WebSocket delivery (handled by Chat Service)
 * 6. Update message status
 *
 * @author WhatsApp Clone Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MessageProcessorApplication {

    public static void main(String[] args) {
        log.info("Starting WhatsApp Clone Message Processor Service...");
        SpringApplication.run(MessageProcessorApplication.class, args);
        log.info("WhatsApp Clone Message Processor Service started successfully!");
    }
}
package com.whatsapp.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler Application
 *
 * Handles scheduled jobs and data cleanup tasks.
 *
 * Key Responsibilities:
 * - Clean up old/undelivered messages
 * - Clean up expired cache entries
 * - Enforce user data retention policies
 * - Generate statistics and reports
 * - Database maintenance tasks
 *
 * Scheduled Jobs:
 * - Message cleanup (daily)
 * - Cache cleanup (hourly)
 * - User policy enforcement (daily)
 * - Statistics generation (hourly)
 *
 * @author WhatsApp Clone Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class SchedulerApplication {

    public static void main(String[] args) {
        log.info("Starting WhatsApp Clone Scheduler Service...");
        SpringApplication.run(SchedulerApplication.class, args);
        log.info("WhatsApp Clone Scheduler Service started successfully!");
    }
}
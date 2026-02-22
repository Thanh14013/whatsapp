package com.whatsapp.scheduler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Scheduler Configuration
 *
 * Configures the task scheduler for executing scheduled jobs.
 *
 * Configuration:
 * - Thread pool size: 10
 * - Thread name prefix: scheduler-
 * - Await termination on shutdown: true
 * - Wait for tasks to complete: 60 seconds
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulerConfig {

    /**
     * Configure task scheduler
     */
    @Bean
    public TaskScheduler taskScheduler() {
        log.info("Configuring task scheduler...");

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // Thread pool configuration
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("scheduler-");

        // Graceful shutdown configuration
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);

        // Error handling
        scheduler.setErrorHandler(throwable ->
                log.error("Error in scheduled task: {}", throwable.getMessage(), throwable)
        );

        scheduler.initialize();

        log.info("Task scheduler configured: poolSize=10, prefix=scheduler-");

        return scheduler;
    }
}
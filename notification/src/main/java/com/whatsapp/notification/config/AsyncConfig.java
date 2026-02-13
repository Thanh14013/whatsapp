package com.whatsapp.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration
 *
 * Configures async execution for notification sending.
 * Allows sending notifications without blocking the main thread.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configure async executor for notifications
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        log.info("Configuring notification async executor...");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Notification async executor configured: core=5, max=10, queue=100");

        return executor;
    }
}
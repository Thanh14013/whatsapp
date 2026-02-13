package com.whatsapp.notification.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Notification Metrics Service
 *
 * Tracks metrics for notification operations.
 * Metrics are exposed via Prometheus endpoint.
 *
 * Metrics:
 * - notification_sent_total: Total notifications sent
 * - notification_failed_total: Total notifications failed
 * - notification_send_duration: Time to send notification
 * - device_token_registered_total: Total tokens registered
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
public class NotificationMetrics {

    private final Counter notificationSentCounter;
    private final Counter notificationFailedCounter;
    private final Counter deviceTokenRegisteredCounter;
    private final Timer notificationSendTimer;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.notificationSentCounter = Counter.builder("notification_sent_total")
                .description("Total number of notifications sent successfully")
                .tag("service", "notification")
                .register(meterRegistry);

        this.notificationFailedCounter = Counter.builder("notification_failed_total")
                .description("Total number of notifications failed")
                .tag("service", "notification")
                .register(meterRegistry);

        this.deviceTokenRegisteredCounter = Counter.builder("device_token_registered_total")
                .description("Total number of device tokens registered")
                .tag("service", "notification")
                .register(meterRegistry);

        this.notificationSendTimer = Timer.builder("notification_send_duration")
                .description("Time taken to send notification")
                .tag("service", "notification")
                .register(meterRegistry);

        log.info("Notification metrics initialized");
    }

    /**
     * Record notification sent
     */
    public void recordNotificationSent() {
        notificationSentCounter.increment();
    }

    /**
     * Record notification failed
     */
    public void recordNotificationFailed() {
        notificationFailedCounter.increment();
    }

    /**
     * Record device token registered
     */
    public void recordDeviceTokenRegistered() {
        deviceTokenRegisteredCounter.increment();
    }

    /**
     * Get timer for notification send duration
     */
    public Timer.Sample startTimer() {
        return Timer.start();
    }

    /**
     * Stop timer and record duration
     */
    public void stopTimer(Timer.Sample sample) {
        sample.stop(notificationSendTimer);
    }
}
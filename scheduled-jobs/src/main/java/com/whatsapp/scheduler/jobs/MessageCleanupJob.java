package com.whatsapp.scheduler.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Message Cleanup Job
 *
 * Scheduled job to clean up old messages based on retention policies.
 *
 * Cleanup Rules:
 * 1. Undelivered messages older than 1 year → DELETE
 * 2. Delivered messages older than user's policy (0-90 days) → DELETE
 * 3. Messages marked as deleted → DELETE after grace period
 *
 * Schedule: Daily at 2:00 AM
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCleanupJob {

    private final MongoTemplate mongoTemplate;

    /**
     * Clean up undelivered messages older than 1 year
     *
     * Runs daily at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupUndeliveredMessages() {
        log.info("Starting cleanup of undelivered messages...");

        try {
            Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);

            Query query = new Query();
            query.addCriteria(
                    Criteria.where("undelivered").is(true)
                            .and("createdAt").lt(oneYearAgo)
            );

            long deletedCount = mongoTemplate.remove(query, "messages").getDeletedCount();

            log.info("Cleaned up {} undelivered messages older than 1 year", deletedCount);

        } catch (Exception e) {
            log.error("Error during undelivered message cleanup", e);
        }
    }

    /**
     * Clean up delivered messages based on retention policy
     *
     * Runs daily at 3:00 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupDeliveredMessages() {
        log.info("Starting cleanup of delivered messages...");

        try {
            // Default: Keep delivered messages for 90 days
            Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);

            Query query = new Query();
            query.addCriteria(
                    Criteria.where("undelivered").ne(true)
                            .and("createdAt").lt(ninetyDaysAgo)
            );

            long deletedCount = mongoTemplate.remove(query, "messages").getDeletedCount();

            log.info("Cleaned up {} delivered messages older than 90 days", deletedCount);

        } catch (Exception e) {
            log.error("Error during delivered message cleanup", e);
        }
    }

    /**
     * Clean up deleted messages after grace period
     *
     * Runs daily at 4:00 AM
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupDeletedMessages() {
        log.info("Starting cleanup of deleted messages...");

        try {
            // Grace period: 30 days after deletion
            Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

            Query query = new Query();
            query.addCriteria(
                    Criteria.where("deleted").is(true)
                            .and("deletedAt").lt(thirtyDaysAgo)
            );

            long deletedCount = mongoTemplate.remove(query, "messages").getDeletedCount();

            log.info("Cleaned up {} deleted messages after grace period", deletedCount);

        } catch (Exception e) {
            log.error("Error during deleted message cleanup", e);
        }
    }

    /**
     * Generate message statistics
     *
     * Runs daily at 5:00 AM
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void generateMessageStatistics() {
        log.info("Generating message statistics...");

        try {
            long totalMessages = mongoTemplate.count(new Query(), "messages");

            Query undeliveredQuery = new Query(Criteria.where("undelivered").is(true));
            long undeliveredMessages = mongoTemplate.count(undeliveredQuery, "messages");

            Query deletedQuery = new Query(Criteria.where("deleted").is(true));
            long deletedMessages = mongoTemplate.count(deletedQuery, "messages");

            log.info("Message Statistics:");
            log.info("  Total messages: {}", totalMessages);
            log.info("  Undelivered messages: {}", undeliveredMessages);
            log.info("  Deleted messages: {}", deletedMessages);

        } catch (Exception e) {
            log.error("Error generating message statistics", e);
        }
    }
}
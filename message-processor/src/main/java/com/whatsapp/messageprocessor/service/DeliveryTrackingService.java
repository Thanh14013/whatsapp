package com.whatsapp.messageprocessor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Delivery Tracking Service
 *
 * Tracks message delivery status in MongoDB.
 * Updates message status and timestamp fields.
 *
 * Status Flow:
 * SENT → DELIVERED → READ
 *   ↓
 * FAILED
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryTrackingService {

    private final MongoTemplate mongoTemplate;

    /**
     * Update message status to DELIVERED
     *
     * @param messageId Message ID
     */
    public void markAsDelivered(String messageId) {
        log.info("Marking message as delivered: {}", messageId);

        try {
            Query query = new Query(Criteria.where("_id").is(messageId));
            Update update = new Update()
                    .set("status", "DELIVERED")
                    .set("deliveredAt", Instant.now())
                    .set("updatedAt", Instant.now());

            mongoTemplate.updateFirst(query, update, "messages");

            log.info("Message marked as delivered: {}", messageId);

        } catch (Exception e) {
            log.error("Error marking message as delivered: {}", messageId, e);
        }
    }

    /**
     * Update message status to READ
     *
     * @param messageId Message ID
     */
    public void markAsRead(String messageId) {
        log.info("Marking message as read: {}", messageId);

        try {
            Query query = new Query(Criteria.where("_id").is(messageId));
            Update update = new Update()
                    .set("status", "READ")
                    .set("readAt", Instant.now())
                    .set("updatedAt", Instant.now());

            mongoTemplate.updateFirst(query, update, "messages");

            log.info("Message marked as read: {}", messageId);

        } catch (Exception e) {
            log.error("Error marking message as read: {}", messageId, e);
        }
    }

    /**
     * Update message status to FAILED
     *
     * @param messageId Message ID
     * @param reason Failure reason
     */
    public void markAsFailed(String messageId, String reason) {
        log.warn("Marking message as failed: {} - {}", messageId, reason);

        try {
            Query query = new Query(Criteria.where("_id").is(messageId));
            Update update = new Update()
                    .set("status", "FAILED")
                    .set("failureReason", reason)
                    .set("updatedAt", Instant.now());

            mongoTemplate.updateFirst(query, update, "messages");

            log.warn("Message marked as failed: {}", messageId);

        } catch (Exception e) {
            log.error("Error marking message as failed: {}", messageId, e);
        }
    }

    /**
     * Get message delivery status
     *
     * @param messageId Message ID
     * @return Delivery status map
     */
    public Map<String, Object> getDeliveryStatus(String messageId) {
        log.debug("Getting delivery status for message: {}", messageId);

        try {
            Query query = new Query(Criteria.where("_id").is(messageId));
            query.fields().include("status", "sentAt", "deliveredAt", "readAt", "failureReason");

            Map<String, Object> result = mongoTemplate.findOne(query, Map.class, "messages");

            if (result != null) {
                log.debug("Delivery status found: {}", result.get("status"));
                return result;
            } else {
                log.warn("Message not found: {}", messageId);
                return new HashMap<>();
            }

        } catch (Exception e) {
            log.error("Error getting delivery status: {}", messageId, e);
            return new HashMap<>();
        }
    }

    /**
     * Check if message is delivered
     *
     * @param messageId Message ID
     * @return true if delivered or read
     */
    public boolean isDelivered(String messageId) {
        try {
            Map<String, Object> status = getDeliveryStatus(messageId);
            String statusValue = (String) status.get("status");

            return "DELIVERED".equals(statusValue) || "READ".equals(statusValue);

        } catch (Exception e) {
            log.error("Error checking if message is delivered: {}", messageId, e);
            return false;
        }
    }

    /**
     * Update delivery attempt count
     *
     * @param messageId Message ID
     */
    public void incrementDeliveryAttempts(String messageId) {
        log.debug("Incrementing delivery attempts for message: {}", messageId);

        try {
            Query query = new Query(Criteria.where("_id").is(messageId));
            Update update = new Update()
                    .inc("deliveryAttempts", 1)
                    .set("lastDeliveryAttempt", Instant.now());

            mongoTemplate.updateFirst(query, update, "messages");

            log.debug("Delivery attempts incremented: {}", messageId);

        } catch (Exception e) {
            log.error("Error incrementing delivery attempts: {}", messageId, e);
        }
    }
}

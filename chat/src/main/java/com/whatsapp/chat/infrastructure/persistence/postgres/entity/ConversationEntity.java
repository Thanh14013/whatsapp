package com.whatsapp.chat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Conversation JPA Entity
 *
 * Stores conversation metadata in PostgreSQL.
 * PostgreSQL is used for relational queries and ACID compliance.
 *
 * @author WhatsApp Clone Team
 */
@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_participant1", columnList = "participant1_id"),
        @Index(name = "idx_participant2", columnList = "participant2_id"),
        @Index(name = "idx_participants_unique", columnList = "participant1_id,participant2_id", unique = true),
        @Index(name = "idx_last_message_at", columnList = "last_message_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEntity {

    @Id
    @Column(name = "id", nullable = false, length = 255)
    private String id;

    @Column(name = "participant1_id", nullable = false, length = 255)
    private String participant1Id;

    @Column(name = "participant2_id", nullable = false, length = 255)
    private String participant2Id;

    @Column(name = "last_message_id", length = 255)
    private String lastMessageId;

    @Column(name = "last_message_content", columnDefinition = "TEXT")
    private String lastMessageContent;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
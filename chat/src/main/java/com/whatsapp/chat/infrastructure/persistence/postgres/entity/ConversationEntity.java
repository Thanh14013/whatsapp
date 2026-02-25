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
import java.util.ArrayList;
import java.util.List;

/**
 * Conversation JPA Entity
 *
 * Stores conversation metadata in PostgreSQL.
 * Participants are stored in a separate {@link ConversationParticipantEntity} table.
 *
 * @author WhatsApp Clone Team
 */
@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversations_type",                    columnList = "type"),
        @Index(name = "idx_conversations_active",                  columnList = "active"),
        @Index(name = "idx_conversations_last_message_timestamp",  columnList = "last_message_timestamp")
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

    @Column(name = "type", nullable = false, length = 20)
    private String type; // ONE_TO_ONE, GROUP, BROADCAST

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "last_message_id", length = 255)
    private String lastMessageId;

    @Column(name = "last_message_timestamp")
    private Instant lastMessageTimestamp;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConversationParticipantEntity> participants = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
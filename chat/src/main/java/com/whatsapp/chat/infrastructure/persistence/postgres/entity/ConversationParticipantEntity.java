package com.whatsapp.chat.infrastructure.persistence.postgres.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Conversation Participant JPA Entity
 *
 * Maps to the {@code conversation_participants} table.
 * Each row represents one user's membership in a conversation.
 *
 * @author WhatsApp Clone Team
 */
@Entity
@Table(name = "conversation_participants", indexes = {
        @Index(name = "idx_conversation_participants_conversation", columnList = "conversation_id"),
        @Index(name = "idx_conversation_participants_user",         columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    @Column(name = "unread_count", nullable = false)
    private int unreadCount = 0;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) joinedAt = Instant.now();
    }
}


package com.whatsapp.chat.infrastructure.persistence.mongodb.document;

import com.whatsapp.chat.domain.model.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Message MongoDB Document
 *
 * Stores message content in MongoDB for scalability.
 * MongoDB is ideal for high-volume message storage.
 *
 * @author WhatsApp Clone Team
 */
@Document(collection = "messages")
@CompoundIndex(name = "conversation_created_idx", def = "{'conversationId': 1, 'createdAt': -1}")
@CompoundIndex(name = "receiver_status_idx", def = "{'receiverId': 1, 'status': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDocument {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    @Indexed
    private String senderId;

    @Indexed
    private String receiverId;

    private String contentText;

    private String contentType; // TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT

    @Indexed
    private String status; // SENT, DELIVERED, READ

    private Boolean deleted;

    @Indexed
    private Instant createdAt;

    private Instant deliveredAt;

    private Instant readAt;

    private Instant deletedAt;
}
package com.whatsapp.chat.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Message Received Response DTO
 *
 * Data Transfer Object sent to a WebSocket client when a new
 * real-time message event is pushed to them.
 * Contains the full message payload plus delivery metadata.
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReceivedResponse {

    /** Unique ID of the message */
    private String messageId;

    /** ID of the conversation this message belongs to */
    private String conversationId;

    /** ID of the user who sent the message */
    private String senderId;

    /** Display name of the sender */
    private String senderName;

    /** ID of the intended recipient */
    private String receiverId;

    /** Content type: TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT */
    private String contentType;

    /** Message text or media URL */
    private String content;

    /** Optional media URL for rich content */
    private String mediaUrl;

    /** Current delivery status: SENT, DELIVERED, READ */
    private String status;

    /** Snowflake-based timestamp when the message was sent */
    private Instant sentAt;

    /** ID of the message this is a reply to, if any */
    private String replyToMessageId;

    /** Whether this message has been soft-deleted */
    private boolean deleted;

    /** Server-side timestamp for ordering */
    private Instant serverTimestamp;
}


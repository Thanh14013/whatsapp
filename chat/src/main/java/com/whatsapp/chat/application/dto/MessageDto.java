package com.whatsapp.chat.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Message DTO
 *
 * Data Transfer Object for message information.
 *
 * @author WhatsApp Clone Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private String id;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String contentType;
    private String content;
    private String mediaUrl;
    private String status;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private String replyToMessageId;
    private boolean deleted;
    private Instant createdAt;
}

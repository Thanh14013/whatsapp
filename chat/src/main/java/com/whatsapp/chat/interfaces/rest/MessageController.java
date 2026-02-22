package com.whatsapp.chat.interfaces.rest;

import com.whatsapp.chat.application.dto.MessageDto;
import com.whatsapp.chat.application.dto.SendMessageRequest;
import com.whatsapp.chat.application.service.ChatApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Message REST Controller
 *
 * Handles HTTP requests for message operations.
 *
 * Endpoints:
 * - POST /messages - Send a new message
 * - GET /messages/{id} - Get message by ID
 * - GET /messages/conversation/{conversationId} - Get conversation messages
 * - PUT /messages/{id}/delivered - Mark message as delivered
 * - PUT /messages/{id}/read - Mark message as read
 * - DELETE /messages/{id} - Delete message
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatApplicationService chatService;

    /**
     * Send a new message
     *
     * POST /api/v1/messages
     */
    @PostMapping
    public ResponseEntity<MessageDto> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        log.info("Sending message from {} to {}", request.getSenderId(), request.getReceiverId());

        MessageDto message = chatService.sendMessage(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /**
     * Get message by ID
     *
     * GET /api/v1/messages/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageDto> getMessage(@PathVariable String id) {
        log.debug("Getting message: {}", id);

        MessageDto message = chatService.getMessage(id);

        return ResponseEntity.ok(message);
    }

    /**
     * Get conversation messages with pagination
     *
     * GET /api/v1/messages/conversation/{conversationId}
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<MessageDto>> getConversationMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.debug("Getting messages for conversation: {}", conversationId);

        List<MessageDto> messages = chatService.getConversationMessages(conversationId, page, size);

        return ResponseEntity.ok(messages);
    }

    /**
     * Mark message as delivered
     *
     * PUT /api/v1/messages/{id}/delivered
     */
    @PutMapping("/{id}/delivered")
    public ResponseEntity<MessageDto> markAsDelivered(
            @PathVariable String id,
            @RequestParam String userId) {

        log.info("Marking message as delivered: {}", id);

        MessageDto message = chatService.markAsDelivered(id, userId);

        return ResponseEntity.ok(message);
    }

    /**
     * Mark message as read
     *
     * PUT /api/v1/messages/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<MessageDto> markAsRead(
            @PathVariable String id,
            @RequestParam String userId) {

        log.info("Marking message as read: {}", id);

        MessageDto message = chatService.markAsRead(id, userId);

        return ResponseEntity.ok(message);
    }

    /**
     * Delete message
     *
     * DELETE /api/v1/messages/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String id,
            @RequestParam String userId) {

        log.info("Deleting message: {}", id);

        chatService.deleteMessage(id, userId);

        return ResponseEntity.noContent().build();
    }
}

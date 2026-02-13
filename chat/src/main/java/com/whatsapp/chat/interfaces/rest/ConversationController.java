package com.whatsapp.chat.interfaces.rest;

import com.whatsapp.chat.application.dto.ConversationDto;
import com.whatsapp.chat.application.dto.CreateConversationRequest;
import com.whatsapp.chat.application.service.ConversationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Conversation REST Controller
 *
 * Handles HTTP requests for conversation operations.
 *
 * Endpoints:
 * - POST /conversations - Create a new conversation
 * - GET /conversations - Get user conversations
 * - GET /conversations/{id} - Get conversation by ID
 * - PUT /conversations/{id}/read - Mark conversation as read
 * - POST /conversations/{id}/participants - Add participant
 * - DELETE /conversations/{id}/participants/{userId} - Remove participant
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * Create a new conversation
     *
     * POST /api/v1/conversations
     */
    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        log.info("Creating new conversation of type: {}", request.getType());

        ConversationDto conversation = conversationService.createConversation(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    /**
     * Get user conversations
     *
     * GET /api/v1/conversations
     */
    @GetMapping
    public ResponseEntity<List<ConversationDto>> getUserConversations(@RequestParam String userId) {
        log.debug("Getting conversations for user: {}", userId);

        List<ConversationDto> conversations = conversationService.getUserConversations(userId);

        return ResponseEntity.ok(conversations);
    }

    /**
     * Get conversation by ID
     *
     * GET /api/v1/conversations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDto> getConversation(
            @PathVariable String id,
            @RequestParam String userId) {

        log.debug("Getting conversation: {}", id);

        ConversationDto conversation = conversationService.getConversation(id, userId);

        return ResponseEntity.ok(conversation);
    }

    /**
     * Mark conversation as read
     *
     * PUT /api/v1/conversations/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String id,
            @RequestParam String userId) {

        log.info("Marking conversation as read: {}", id);

        conversationService.markAsRead(id, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Add participant to group conversation
     *
     * POST /api/v1/conversations/{id}/participants
     */
    @PostMapping("/{id}/participants")
    public ResponseEntity<ConversationDto> addParticipant(
            @PathVariable String id,
            @RequestParam String userId,
            @RequestParam String newParticipantId,
            @RequestParam String newParticipantName) {

        log.info("Adding participant {} to conversation: {}", newParticipantId, id);

        ConversationDto conversation = conversationService.addParticipant(id, userId, newParticipantId,
                newParticipantName);

        return ResponseEntity.ok(conversation);
    }

    /**
     * Remove participant from group conversation
     *
     * DELETE /api/v1/conversations/{id}/participants/{participantId}
     */
    @DeleteMapping("/{id}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable String id,
            @PathVariable String participantId,
            @RequestParam String userId) {

        log.info("Removing participant {} from conversation: {}", participantId, id);

        conversationService.removeParticipant(id, userId, participantId);

        return ResponseEntity.noContent().build();
    }
}

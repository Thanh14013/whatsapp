package com.whatsapp.chat.infrastructure.persistence.mongodb.repository;

import com.whatsapp.chat.domain.model.Message;
import com.whatsapp.chat.domain.model.MessageStatus;
import com.whatsapp.chat.domain.model.vo.ConversationId;
import com.whatsapp.chat.domain.model.vo.MessageContent;
import com.whatsapp.chat.domain.model.vo.MessageId;
import com.whatsapp.chat.domain.repository.MessageRepository;
import com.whatsapp.chat.infrastructure.persistence.mongodb.document.MessageDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Message Repository Implementation
 *
 * Adapts the domain {@link MessageRepository} interface to MongoDB
 * using {@link MessageMongoRepository}.
 *
 * Mapping strategy:
 *  Message domain model  ↔  MessageDocument (MongoDB)
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageMongoRepository mongoRepository;

    // ---------------------------------------------------------------
    // MessageRepository implementation
    // ---------------------------------------------------------------

    @Override
    public Message save(Message message) {
        MessageDocument doc = toDocument(message);
        MessageDocument saved = mongoRepository.save(doc);
        return toDomain(saved);
    }

    @Override
    public Optional<Message> findById(MessageId messageId) {
        return mongoRepository.findById(messageId.getValue()).map(this::toDomain);
    }

    @Override
    public List<Message> findByConversationId(ConversationId conversationId, int limit) {
        return mongoRepository.findByConversationIdOrderByCreatedAtDesc(
                        conversationId.getValue(),
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Message> findByConversationId(ConversationId conversationId, int offset, int limit) {
        int page = offset / limit;
        return mongoRepository.findByConversationIdOrderByCreatedAtDesc(
                        conversationId.getValue(),
                        PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Message> findUndeliveredMessages(String receiverId) {
        return mongoRepository.findByReceiverIdAndStatusOrderByCreatedAtAsc(receiverId, MessageStatus.SENT.name())
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Message> findBySenderAndReceiver(String senderId, String receiverId, int limit) {
        return mongoRepository.findBetweenUsers(senderId, receiverId,
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByConversationId(ConversationId conversationId) {
        return mongoRepository.countByConversationId(conversationId.getValue());
    }

    @Override
    public long countUndeliveredMessages(String receiverId) {
        return mongoRepository.countByReceiverIdAndStatus(receiverId, MessageStatus.SENT.name());
    }

    @Override
    public void delete(MessageId messageId) {
        mongoRepository.deleteById(messageId.getValue());
    }

    @Override
    public void deleteByConversationId(ConversationId conversationId) {
        mongoRepository.deleteByConversationId(conversationId.getValue());
    }

    // ---------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------

    private MessageDocument toDocument(Message domain) {
        return MessageDocument.builder()
                .id(domain.getId().getValue())
                .conversationId(domain.getConversationId().getValue())
                .senderId(domain.getSenderId())
                .receiverId(domain.getReceiverId())
                .contentText(domain.getContent().getText())
                .contentType(domain.getContent().getType().name())
                .status(domain.getStatus().name())
                .deleted(domain.isDeleted())
                .createdAt(domain.getCreatedAt())
                .deliveredAt(domain.getDeliveredAt())
                .readAt(domain.getReadAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }

    private Message toDomain(MessageDocument doc) {
        MessageContent content = buildContent(doc.getContentText(), doc.getContentType());

        return Message.reconstitute(
                MessageId.of(doc.getId()),
                ConversationId.of(doc.getConversationId()),
                doc.getSenderId(),
                doc.getReceiverId(),
                content,
                MessageStatus.valueOf(doc.getStatus()),
                Boolean.TRUE.equals(doc.getDeleted()),
                null,          // replyToMessageId – extend MessageDocument if reply chains needed
                doc.getCreatedAt(),
                doc.getCreatedAt(),
                doc.getDeliveredAt(),
                doc.getReadAt(),
                doc.getDeletedAt()
        );
    }

    private MessageContent buildContent(String text, String type) {
        if (type == null) return MessageContent.text(text != null ? text : "");
        return switch (type) {
            case "IMAGE"    -> MessageContent.image(text);
            case "VIDEO"    -> MessageContent.video(text);
            case "AUDIO"    -> MessageContent.audio(text);
            case "DOCUMENT" -> MessageContent.document(text);
            default         -> MessageContent.text(text != null ? text : "");
        };
    }
}


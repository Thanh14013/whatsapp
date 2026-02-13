package com.whatsapp.chat.domain.model.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * MessageContent Value Object
 *
 * Represents the content of a message.
 * Immutable value object with validation.
 *
 * Content Types:
 * - TEXT: Plain text message
 * - IMAGE: Image URL
 * - VIDEO: Video URL
 * - AUDIO: Audio URL
 * - DOCUMENT: Document URL
 *
 * @author WhatsApp Clone Team
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageContent {

    private static final int MAX_TEXT_LENGTH = 10000; // 10k characters

    private String text;
    private MessageContentType type;

    /**
     * Create text message content
     */
    public static MessageContent text(String text) {
        validateText(text);
        return new MessageContent(text, MessageContentType.TEXT);
    }

    /**
     * Create image message content
     */
    public static MessageContent image(String imageUrl) {
        validateUrl(imageUrl);
        return new MessageContent(imageUrl, MessageContentType.IMAGE);
    }

    /**
     * Create video message content
     */
    public static MessageContent video(String videoUrl) {
        validateUrl(videoUrl);
        return new MessageContent(videoUrl, MessageContentType.VIDEO);
    }

    /**
     * Create audio message content
     */
    public static MessageContent audio(String audioUrl) {
        validateUrl(audioUrl);
        return new MessageContent(audioUrl, MessageContentType.AUDIO);
    }

    /**
     * Create document message content
     */
    public static MessageContent document(String documentUrl) {
        validateUrl(documentUrl);
        return new MessageContent(documentUrl, MessageContentType.DOCUMENT);
    }

    /**
     * Check if content is text
     */
    public boolean isText() {
        return type == MessageContentType.TEXT;
    }

    /**
     * Check if content is media (image, video, audio)
     */
    public boolean isMedia() {
        return type == MessageContentType.IMAGE ||
                type == MessageContentType.VIDEO ||
                type == MessageContentType.AUDIO;
    }

    /**
     * Validate text content
     */
    private static void validateText(String text) {
        Objects.requireNonNull(text, "Text cannot be null");

        if (text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }

        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Text cannot exceed " + MAX_TEXT_LENGTH + " characters");
        }
    }

    /**
     * Validate URL content
     */
    private static void validateUrl(String url) {
        Objects.requireNonNull(url, "URL cannot be null");

        if (url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        // Basic URL validation
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageContent that = (MessageContent) o;
        return Objects.equals(text, that.text) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, type);
    }

    /**
     * MessageContentType Enum
     */
    public enum MessageContentType {
        TEXT,
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT
    }
}
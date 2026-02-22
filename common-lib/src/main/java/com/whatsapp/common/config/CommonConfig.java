package com.whatsapp.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Common Configuration
 *
 * Shared configurations used across all microservices.
 *
 * @author WhatsApp Clone Team
 */
@Configuration
public class CommonConfig {

    /**
     * Configure ObjectMapper for JSON serialization
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java 8 Date/Time module
        mapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps (use ISO-8601 instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Configure other features
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        return mapper;
    }
}
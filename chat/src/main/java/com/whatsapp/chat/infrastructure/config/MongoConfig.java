package com.whatsapp.chat.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB Configuration
 *
 * Configures MongoDB for message storage.
 *
 * @author WhatsApp Clone Team
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.whatsapp.chat.infrastructure.persistence.mongodb.repository")
public class MongoConfig {
    // MongoDB configuration is primarily in application.yaml
    // This class enables auditing and repository scanning
}

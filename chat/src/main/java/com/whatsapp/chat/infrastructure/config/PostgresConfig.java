package com.whatsapp.chat.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * PostgreSQL / JPA Configuration
 *
 * Enables:
 * - JPA repository scanning for the postgres persistence layer
 * - Auditing (auto-populate @CreatedDate / @LastModifiedDate)
 * - Declarative transaction management
 *
 * Connection pool settings and dialect are defined in application.yaml.
 *
 * @author WhatsApp Clone Team
 */
@Configuration
@EnableJpaAuditing
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.whatsapp.chat.infrastructure.persistence.postgres.repository"
)
public class PostgresConfig {
    // All datasource / JPA properties are configured via application.yaml.
    // This class activates auditing and repository scanning only.
}


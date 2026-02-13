package com.whatsapp.user.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Database Configuration
 *
 * Configures PostgreSQL database connection with HikariCP.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.whatsapp.user.infrastructure.persistence.repository")
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    /**
     * Configure HikariCP DataSource
     */
    @Bean
    public DataSource dataSource() {
        log.info("Configuring HikariCP DataSource...");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(600000); // 10 minutes
        config.setPoolName("UserServiceHikariPool");

        // PostgreSQL optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        log.info("HikariCP DataSource configured successfully");

        return new HikariDataSource(config);
    }
}
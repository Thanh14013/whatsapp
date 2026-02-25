package com.whatsapp.chat.infrastructure.idgen;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Snowflake ID Generator
 *
 * Generates globally unique, time-ordered 64-bit IDs using the
 * Twitter Snowflake algorithm via the Hutool library.
 *
 * Snowflake ID layout (64 bits):
 * ┌──────────────────────────────────────────┬─────────────┬────────────────┐
 * │  41-bit timestamp (ms since epoch)       │  10-bit node│  12-bit seq    │
 * └──────────────────────────────────────────┴─────────────┴────────────────┘
 *
 * Properties:
 * - Monotonically increasing within a node  → naturally sortable
 * - Unique across up to 1024 worker nodes   → distributed-safe
 * - ~4 million IDs per second per node      → high throughput
 *
 * Configuration (application.yaml):
 *   app.idgen.worker-id   – 0-31  (worker identifier within datacenter)
 *   app.idgen.datacenter-id – 0-31
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
public class SnowflakeIdGenerator {

    private final Snowflake snowflake;

    public SnowflakeIdGenerator(
            @Value("${app.idgen.worker-id:1}") long workerId,
            @Value("${app.idgen.datacenter-id:1}") long datacenterId) {

        this.snowflake = IdUtil.getSnowflake(workerId, datacenterId);
        log.info("SnowflakeIdGenerator initialised – workerId={} datacenterId={}", workerId, datacenterId);
    }

    /**
     * Generate the next Snowflake ID as a {@code long}.
     *
     * @return unique 64-bit long ID
     */
    public long nextId() {
        return snowflake.nextId();
    }

    /**
     * Generate the next Snowflake ID as a {@link String}.
     * Preferred for use as a database primary key to avoid JS precision issues.
     *
     * @return unique ID string
     */
    public String nextIdStr() {
        return snowflake.nextIdStr();
    }

    /**
     * Extract the generation timestamp (milliseconds since epoch) from a Snowflake ID.
     *
     * @param id the snowflake ID
     * @return epoch millis at which the ID was generated
     */
    public long extractTimestamp(long id) {
        // Hutool's Snowflake uses a custom epoch; delegate to the library
        return snowflake.getGenerateDateTime(id);
    }
}


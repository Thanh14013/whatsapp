package com.whatsapp.commonlib;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Common Library Auto-Configuration.
 * <p>
 * Entry point for Spring Boot auto-configuration of common-lib.
 * Consumers of this library just need to add it as a Maven dependency –
 * Spring Boot will automatically pick up this configuration via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 * </p>
 *
 * @author WhatsApp Clone Team
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.whatsapp.common")
public class CommonLibApplication {
    // Auto-configuration marker class – no additional beans needed here.
    // Beans are declared in com.whatsapp.common.config.CommonConfig
}

package com.whatsapp.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.whatsapp.gateway.filter.AuthenticationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration for API Gateway
 *
 * Configures:
 * - JWT-based authentication
 * - CORS policies
 * - Public/Private endpoints
 * - WebSocket security
 * - CSRF protection (disabled for stateless API)
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationFilter authenticationFilter;

    /**
     * Main security filter chain configuration
     *
     * Public Endpoints (No Authentication Required):
     * - POST /api/auth/login
     * - POST /api/auth/register
     * - GET /actuator/health
     * - GET /actuator/info
     *
     * Protected Endpoints (Authentication Required):
     * - All other /api/** endpoints
     * - All WebSocket connections /ws/**
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Configuring Security Web Filter Chain...");

        return http
                // Disable CSRF for stateless REST API
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configure authorization rules
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints - no authentication required
                        .pathMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.GET, "/actuator/prometheus").permitAll()

                        // Fallback endpoints
                        .pathMatchers("/fallback/**").permitAll()

                        // All other requests require authentication
                        .anyExchange().authenticated()
                )

                // Add custom JWT authentication filter
                .addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // Stateless session management (no session, JWT only)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // Disable HTTP Basic Authentication
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // Disable form login
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Disable logout (handled by client-side token removal)
                .logout(ServerHttpSecurity.LogoutSpec::disable)

                .build();
    }

    /**
     * CORS Configuration
     *
     * Allows requests from:
     * - Web clients (http://localhost:3000, http://localhost:4200)
     * - Mobile apps (configured origins)
     * - Production domains
     *
     * Allowed Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
     * Allowed Headers: All headers including Authorization
     * Max Age: 3600 seconds (1 hour)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS...");

        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins (modify for production)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",      // React frontend
                "http://localhost:4200",      // Angular frontend
                "http://localhost:8080",      // Local development
                "https://*.yourdomain.com"    // Production domain
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Exposed headers (headers that browser can access)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Max age for preflight requests (1 hour)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Password encoder for user authentication
     * Uses BCrypt with strength 12
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
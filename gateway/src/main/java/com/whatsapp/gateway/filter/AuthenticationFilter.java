package com.whatsapp.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for API Gateway
 *
 * This filter intercepts all incoming requests and validates JWT tokens.
 *
 * Authentication Flow:
 * 1. Extract JWT token from Authorization header (Bearer token)
 * 2. Validate token signature and expiration
 * 3. Extract user claims (userId, username, roles)
 * 4. Create Spring Security Authentication object
 * 5. Store authentication in SecurityContext
 * 6. Add user information to request headers for downstream services
 *
 * Token Format:
 * Authorization: Bearer <JWT_TOKEN>
 *
 * JWT Claims:
 * - sub: User ID
 * - username: Username
 * - email: User email
 * - roles: List of user roles
 * - iat: Issued at timestamp
 * - exp: Expiration timestamp
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
public class AuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Value("${jwt.secret:your-256-bit-secret-your-256-bit-secret-your-256-bit-secret-your-256-bit-secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpiration;

    /**
     * Main filter method
     * Validates JWT token and sets authentication context
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint accessed: {}", path);
            return chain.filter(exchange);
        }

        // Extract JWT token from header
        String token = extractToken(request);

        if (token == null) {
            log.warn("No JWT token found in request to: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            // Validate and parse JWT token
            Claims claims = validateAndParseToken(token);

            // Extract user information from claims
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            List<String> roles = claims.get("roles", List.class);

            log.debug("Authenticated user: {} (ID: {})", username, userId);

            // Create Spring Security Authentication object
            Authentication authentication = createAuthentication(userId, username, roles);

            // Add user information to request headers for downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(USER_ID_HEADER, userId)
                    .header(USERNAME_HEADER, username)
                    .header(USER_ROLES_HEADER, String.join(",", roles))
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            // Set authentication in reactive security context and continue filter chain
            return chain.filter(mutatedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for request to: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();

        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            log.error("Invalid JWT token for request to: {}", path, e);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();

        } catch (Exception e) {
            log.error("JWT authentication error for request to: {}", path, e);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * Extract JWT token from Authorization header
     *
     * @param request HTTP request
     * @return JWT token or null if not found
     */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Validate JWT token and extract claims
     *
     * @param token JWT token
     * @return Claims extracted from token
     * @throws JwtException if token is invalid
     */
    private Claims validateAndParseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Create Spring Security Authentication object from user claims
     *
     * @param userId User ID
     * @param username Username
     * @param roles User roles
     * @return Authentication object
     */
    private Authentication createAuthentication(String userId, String username, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }

    /**
     * Check if endpoint is public (no authentication required)
     *
     * @param path Request path
     * @return true if endpoint is public
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/actuator/health") ||
                path.startsWith("/actuator/info") ||
                path.startsWith("/actuator/prometheus") ||
                path.startsWith("/fallback/");
    }
}
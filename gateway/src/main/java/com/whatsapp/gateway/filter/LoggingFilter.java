package com.whatsapp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Global Logging Filter for API Gateway
 *
 * Logs all incoming requests and outgoing responses with detailed information:
 * - Request ID (correlation ID for tracing)
 * - HTTP method and path
 * - Client IP address
 * - User agent
 * - Request headers (filtered for security)
 * - Response status code
 * - Response time (latency)
 *
 * This filter runs first in the filter chain (HIGHEST_PRECEDENCE)
 * to ensure accurate timing measurements.
 *
 * Log Format:
 * [REQUEST] [request-id] [method] [path] from [ip]
 * [RESPONSE] [request-id] [status] in [duration]ms
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final String REQUEST_ID_ATTRIBUTE = "requestId";

    /**
     * Filter order: Highest precedence to run first
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Main filter method
     * Logs request and response with timing information
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate or extract request ID for correlation
        String requestId = extractOrGenerateRequestId(request);

        // Store request ID and start time in exchange attributes
        exchange.getAttributes().put(REQUEST_ID_ATTRIBUTE, requestId);
        exchange.getAttributes().put(START_TIME_ATTRIBUTE, Instant.now());

        // Add request ID to response headers for client tracking
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        // Log incoming request
        logRequest(request, requestId);

        // Continue filter chain and log response when complete
        return chain.filter(exchange)
                .doOnSuccess(aVoid -> logResponse(exchange, requestId))
                .doOnError(throwable -> logError(exchange, requestId, throwable));
    }

    /**
     * Extract existing request ID from header or generate new one
     */
    private String extractOrGenerateRequestId(ServerHttpRequest request) {
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);

        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        return requestId;
    }

    /**
     * Log incoming request details
     */
    private void logRequest(ServerHttpRequest request, String requestId) {
        HttpMethod method = request.getMethod();
        String path = request.getPath().value();
        String clientIp = extractClientIp(request);
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);

        log.info("[REQUEST] [{}] {} {} from {} | User-Agent: {}",
                requestId,
                method,
                path,
                clientIp,
                userAgent != null ? userAgent : "Unknown");

        // Log query parameters if present (for debugging)
        if (!request.getQueryParams().isEmpty()) {
            log.debug("[REQUEST] [{}] Query Parameters: {}", requestId, request.getQueryParams());
        }
    }

    /**
     * Log outgoing response with timing
     */
    private void logResponse(ServerWebExchange exchange, String requestId) {
        ServerHttpResponse response = exchange.getResponse();
        Instant startTime = exchange.getAttribute(START_TIME_ATTRIBUTE);

        if (startTime != null) {
            long duration = Duration.between(startTime, Instant.now()).toMillis();

            log.info("[RESPONSE] [{}] Status: {} | Duration: {}ms",
                    requestId,
                    response.getStatusCode(),
                    duration);

            // Warn if request takes too long (> 2 seconds)
            if (duration > 2000) {
                log.warn("[SLOW REQUEST] [{}] took {}ms to complete", requestId, duration);
            }
        } else {
            log.info("[RESPONSE] [{}] Status: {}", requestId, response.getStatusCode());
        }
    }

    /**
     * Log errors that occur during request processing
     */
    private void logError(ServerWebExchange exchange, String requestId, Throwable error) {
        ServerHttpRequest request = exchange.getRequest();
        Instant startTime = exchange.getAttribute(START_TIME_ATTRIBUTE);

        long duration = startTime != null
                ? Duration.between(startTime, Instant.now()).toMillis()
                : 0;

        log.error("[ERROR] [{}] {} {} failed after {}ms | Error: {}",
                requestId,
                request.getMethod(),
                request.getPath().value(),
                duration,
                error.getMessage(),
                error);
    }

    /**
     * Extract client IP address from request
     * Considers X-Forwarded-For header for proxied requests
     */
    private String extractClientIp(ServerHttpRequest request) {
        // Check X-Forwarded-For header (for proxied requests)
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fallback to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "Unknown";
    }
}
# ===================================================================
# Multi-stage Dockerfile for WhatsApp Clone API Gateway
# ===================================================================
# Stage 1: Build Stage
# ===================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy parent POM first (for better layer caching)
COPY pom.xml .

# Copy common-lib module
COPY common-lib ./common-lib

# Copy api-gateway module
COPY api-gateway/pom.xml ./api-gateway/
COPY api-gateway/src ./api-gateway/src

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -pl api-gateway -am

# ===================================================================
# Stage 2: Runtime Stage
# ===================================================================
FROM eclipse-temurin:17-jre-alpine

# Install additional utilities
RUN apk add --no-cache \
    curl \
    bash \
    tzdata

# Set timezone
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/api-gateway/target/api-gateway.jar /app/api-gateway.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-Xms256m -Xmx512m \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=docker"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/api-gateway.jar"]
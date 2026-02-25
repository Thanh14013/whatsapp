# 🚀 Skills & Technologies — WhatsApp Clone (Production-Grade Microservices)

> Tổng hợp toàn bộ kiến thức & kỹ thuật được áp dụng trong dự án **WhatsApp Clone** — một hệ thống nhắn tin thời gian thực, production-ready, thiết kế cho **100 triệu DAU** với **230K RPS** peak.

---

## 1. 🏗️ System Design & Architecture

| Kỹ thuật | Mô tả |
|---|---|
| **Microservices Architecture** | Tách hệ thống thành 6 service độc lập: API Gateway, User, Chat, Message Processor, Notification, Scheduled Jobs |
| **Domain-Driven Design (DDD)** | Áp dụng Aggregate Root, Value Objects, Domain Services, Repository pattern — mỗi service là một Bounded Context riêng |
| **Hexagonal Architecture (Ports & Adapters)** | Tách biệt Domain Layer / Application Layer / Infrastructure Layer / Interface Layer — dễ test, dễ thay thế |
| **Event-Driven Architecture** | Services giao tiếp qua RabbitMQ events, không phụ thuộc trực tiếp lẫn nhau (loose coupling) |
| **CQRS (Command Query Responsibility Segregation)** | Tách riêng command (write) và query (read) service; e.g. `ChatApplicationService` vs `MessageQueryService` |
| **Dual Database Pattern** | PostgreSQL cho metadata cần ACID, MongoDB cho message content cần scale cao — mỗi loại DB dùng đúng thế mạnh |
| **Polyglot Persistence** | Cùng một service (Chat) dùng đồng thời PostgreSQL + MongoDB + Redis |
| **API Gateway Pattern** | Single entry point xử lý routing, auth, rate limiting, circuit breaking |
| **Shared Kernel (common-lib)** | Shared library tái sử dụng DTOs, exceptions, utils giữa các services |
| **Capacity Estimation** | Tính toán tải: 100M DAU × 20 msg/day = 2B msg/day → 23K avg RPS → 230K peak RPS; 50TB storage cho 4 tháng |

---

## 2. ☕ Java & Spring Ecosystem

| Kỹ thuật | Mô tả |
|---|---|
| **Java 17** | Records, Sealed Classes, Text Blocks, Switch Expressions, Pattern Matching |
| **Spring Boot 3.2.2** | Auto-configuration, embedded server, production-ready features |
| **Spring Cloud Gateway 2023.0.0** | Reactive API Gateway dựa trên Netty/WebFlux — non-blocking, high throughput |
| **Spring WebFlux** | Reactive programming model trong Gateway (Mono/Flux) |
| **Spring Data JPA 3.2** | ORM PostgreSQL với HikariCP connection pool (max 10 per service) |
| **Spring Data MongoDB** | Document-based persistence cho message content |
| **Spring Data Redis (Lettuce)** | Redis client với connection pooling (Lettuce async driver) |
| **Spring AMQP 3.1** | RabbitMQ integration — `RabbitTemplate`, `@RabbitListener`, `Jackson2JsonMessageConverter` |
| **Spring WebSocket + STOMP** | Real-time messaging với `@EnableWebSocketMessageBroker`, topic/queue destinations |
| **Spring Security 6.2 (Reactive)** | Stateless JWT security với `@EnableWebFluxSecurity`, `ServerHttpSecurity`, `NoOpServerSecurityContextRepository` |
| **Spring Actuator** | Health checks, metrics export (`/actuator/health`, `/actuator/prometheus`) |
| **Spring Scheduling** | `@Scheduled` cron jobs cho cleanup tasks |
| **Maven Multi-Module** | Parent POM quản lý 7 modules, `<dependencyManagement>`, BOM import, Maven profiles (dev/prod/skip-tests) |
| **Flyway 10.7** | Database migration, version control cho schema PostgreSQL |
| **Spring Cache** | `@Cacheable` với Redis backend, TTL configuration |
| **Spring Boot Profiles** | `application.yaml` + `application-prod.yml` + `application-docker.yml` |

---

## 3. 🔐 Security

| Kỹ thuật | Mô tả |
|---|---|
| **JWT (JJWT 0.12.5)** | HS256 token generation/validation, 24h expiry, refresh token 7 ngày |
| **BCrypt Password Hashing** | Strength 12 — không bao giờ lưu plain-text password |
| **Stateless Authentication** | Không dùng session server-side, JWT đủ để authenticate mọi request |
| **Custom WebFilter (Reactive)** | `AuthenticationFilter implements WebFilter` — inject `X-User-Id`, `X-Username`, `X-User-Roles` headers xuống downstream |
| **Role-Based Access Control (RBAC)** | `SimpleGrantedAuthority` từ JWT claims, `pathMatchers().hasRole()` |
| **Account Lockout** | 5 lần đăng nhập sai → khóa 15 phút |
| **Password Policy Enforcement** | 8+ ký tự, uppercase, lowercase, digit, special char |
| **TLS/SSL Termination** | Nginx xử lý HTTPS, nội bộ Docker network là HTTP |
| **CORS Configuration** | Global CORS config trong Gateway |
| **CSRF Disabled** | Hợp lý cho stateless REST/WebSocket API |

---

## 4. ⚡ Real-Time Communication

| Kỹ thuật | Mô tả |
|---|---|
| **WebSocket Protocol** | Full-duplex persistent connection giữa client và Chat Service |
| **STOMP Protocol over WebSocket** | Structured messaging protocol với destinations (`/app/*`, `/topic/*`, `/queue/*`, `/user/*`) |
| **SockJS Fallback** | Fallback cho browser không support native WebSocket |
| **WebSocket Session Management** | `WebSocketSessionManager` — track active connections |
| **Connection Registry (Redis)** | `user:instance:{userId}` mapping để route message đúng Chat Service instance khi scale horizontally |
| **Redis Pub/Sub** | Inter-instance broadcast — khi scale Chat Service lên nhiều pods, message được forward qua Redis channel |
| **Sticky Sessions (Nginx)** | Consistent hash by `userId` để cùng user luôn vào cùng một Chat Service instance |
| **Heartbeat / Ping-Pong** | Client ping mỗi 30s, server pong — detect dead connections |
| **Message Frame Contract** | Định nghĩa chuẩn JSON frames: `send_message`, `message_delivered`, `message_read`, `incoming_message`, `user_status` |

---

## 5. 📨 Messaging & Event-Driven

| Kỹ thuật | Mô tả |
|---|---|
| **RabbitMQ 3.12** | Message broker cho async event processing |
| **Direct Exchange + Topic Exchange** | `user.events` (topic), `message.events` (direct), `notification.events` (direct) |
| **Durable Queues** | Messages không mất khi service restart |
| **Dead Letter Queue (DLQ)** | Xử lý messages failed sau max retry |
| **Message Retry with Backoff** | 3 retries, initial interval 1s, multiplier 2.0, max 10s |
| **Jackson2JsonMessageConverter** | Serialize/deserialize Java objects → JSON trong RabbitMQ messages |
| **Event Publishing Pattern** | `MessageEventPublisher` — publish `MESSAGE_SENT`, `MESSAGE_DELIVERED`, `MESSAGE_READ` |
| **Consumer Groups** | Mỗi service declare queue riêng, bind vào exchange với routing key |

---

## 6. 🗄️ Databases & Data Engineering

### PostgreSQL 15
| Kỹ thuật | Mô tả |
|---|---|
| **ACID Transactions** | Đảm bảo consistency cho user data và conversation metadata |
| **Database Schema Design** | `users`, `conversations`, `conversation_participants` với foreign keys, indexes |
| **Index Optimization** | `idx_users_username`, `idx_users_email`, `idx_participants_user` |
| **HikariCP Connection Pool** | max-pool-size: 10, min-idle: 5, connection-timeout: 30s |
| **JPA Batch Operations** | `batch_size: 20`, `order_inserts: true`, `order_updates: true` |
| **Flyway Migration** | Versioned SQL migration scripts, `baseline-on-migrate` |
| **Read/Write Separation** | Read replicas cho GET queries khi scale |
| **Table Partitioning** | Partition by `user_id` range khi data lớn |

### MongoDB 7.0
| Kỹ thuật | Mô tả |
|---|---|
| **Document Data Modeling** | Schema-less messages với các fields: conversationId, senderId, contentType, status, timestamps |
| **Compound Index** | `{ conversationId: 1, createdAt: -1 }` cho pagination; `{ senderId: 1, createdAt: -1 }` cho sent history |
| **TTL Index** | `expireAfterSeconds: 7776000` — tự động xóa messages sau 90 ngày |
| **Shard Key Design** | `{ conversationId: 1, createdAt: 1 }` — phân tán đều data trên nhiều shards |
| **Replica Set** | 1 Primary + 2 Secondaries — high availability |
| **MongoTemplate** | Low-level query API cho complex queries trong cleanup jobs (`Criteria.where().and()`) |
| **Auto Index Creation** | `auto-index-creation: true` trong dev |
| **Snowflake ID** | Distributed ID generation — sortable, globally unique (không dùng MongoDB ObjectId) |

### Redis 7.2
| Kỹ thuật | Mô tả |
|---|---|
| **Redis Data Structures** | Hash (user profile), Set (device tokens, inbox IDs), String+TTL (user status, rate limit) |
| **TTL Management** | User status: 5 min; User cache: 1h; Device tokens: 30 days; Inbox: 1 year; Session: 24h |
| **Write-Through Cache** | Update Redis đồng thời với DB write |
| **Cache Invalidation** | Evict cache khi user update profile |
| **Lettuce Connection Pool** | Async Redis client, max-active: 8, min-idle: 2 |
| **Redis Append-Only File (AOF)** | Persistence mode: `--appendonly yes` |
| **RedisTemplate** | String serializer cho keys/values |
| **Multi-Level Cache** | L1: Redis (ms) → L2: PostgreSQL/MongoDB (ms~s) |

---

## 7. 🔧 Rate Limiting & Resilience

| Kỹ thuật | Mô tả |
|---|---|
| **Bucket4j 8.x** | Token Bucket algorithm cho rate limiting — in-memory với `ConcurrentHashMap` |
| **Token Bucket Algorithm** | Capacity + Refill rate: anonymous 10 req/min, authenticated 100 req/min, premium 500 req/min |
| **Resilience4j** | Circuit Breaker pattern — open after 50% failures trong 10s window |
| **Circuit Breaker States** | CLOSED → OPEN → HALF_OPEN → CLOSED |
| **Retry with Exponential Backoff** | 3 retries, exponential back-off trong Gateway |
| **Nginx Rate Limiting** | `limit_req_zone` — 100 req/s API, 10 req/s auth endpoints |
| **Connection Limiting** | `limit_conn_zone` tại Nginx layer |
| **Graceful Shutdown** | `server.shutdown: graceful` — drain in-flight requests trước khi shutdown |

---

## 8. 📦 Containerization & Deployment

| Kỹ thuật | Mô tả |
|---|---|
| **Docker 24.0** | Containerize từng microservice với Dockerfile multi-stage build |
| **Docker Compose 2.23** | Orchestrate toàn bộ 16 containers (6 services + 4 DBs + 5 monitoring + Nginx) |
| **Docker Networking** | Custom bridge network `whatsapp-network` — service discovery bằng container name |
| **Docker Volumes** | Named volumes: `postgres-data`, `mongodb-data`, `redis-data`, `rabbitmq-data`, `prometheus-data`, `grafana-data`, `elasticsearch-data` |
| **Docker Health Checks** | `healthcheck` cho mọi service — Kubernetes-style liveness/readiness |
| **Environment Variable Injection** | Config qua env vars (`SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST`, etc.) |
| **Docker Depends-On** | Dependency ordering giữa services |
| **Nginx Reverse Proxy** | Load balancing với `least_conn`, upstream health check, keepalive 32, gzip compression |
| **Kubernetes Ready** | Có thư mục `kubernetes/` với deployments, services, configmaps — sẵn sàng migrate lên K8s |
| **Multi-environment Profiles** | dev / docker / prod profiles — `SPRING_PROFILES_ACTIVE` |

---

## 9. 📊 Observability & Monitoring

| Kỹ thuật | Mô tả |
|---|---|
| **Micrometer 1.12** | Metrics abstraction layer — custom counters, timers, gauges |
| **Prometheus** | Pull-based metrics collection, scrape interval 15s, relabeling |
| **Grafana** | Visualization dashboards — Spring Boot Stats (ID:10280), JVM Micrometer (ID:4701), RabbitMQ (ID:10991) |
| **Custom Business Metrics** | `notification_sent_total`, `notification_failed_total`, `notification_send_duration`, `device_token_registered_total`, `websocket_connections_active` |
| **ELK Stack** | Elasticsearch 8.11 + Logstash + Kibana — centralized log management |
| **Logstash Pipeline** | TCP JSON input → parse → mutate → tag → output to Elasticsearch |
| **Structured JSON Logging** | Logs include: timestamp, level, service, traceId, userId, message |
| **Spring Boot Actuator** | `/actuator/health`, `/actuator/prometheus`, `/actuator/info` — liveness & readiness probes |
| **HikariCP Metrics** | `hikaricp_connections_active`, `hikaricp_connections_idle`, `hikaricp_connections_pending` |
| **Key Metrics Tracked** | `http_server_requests_seconds`, `jvm_memory_used_bytes`, `cache_gets_total{result=hit|miss}`, `spring_rabbitmq_listener_seconds` |

---

## 10. 🔔 Push Notification System

| Kỹ thuật | Mô tả |
|---|---|
| **Firebase Admin SDK 9.2.0** | FCM push notifications cho Android và Web |
| **APNs (Apple Push Notification service)** | iOS push notifications via `APNSNotificationService` |
| **Multi-Device Support** | 1 user → multiple device tokens (phone + tablet + web); lưu trong Redis Set |
| **Batch Notification** | `sendEachForMulticast()` — gửi đến nhiều tokens cùng lúc |
| **Topic-based Notification** | FCM Topic messaging |
| **Invalid Token Cleanup** | FCM `UNREGISTERED`/`INVALID_REGISTRATION` response → auto-remove token khỏi Redis |
| **Notification Payload** | Structured payload: sender name, message preview, conversationId, badge, sound |
| **Offline Detection** | Message Processor check `user:status:{id}` trong Redis → nếu không có key thì user OFFLINE |

---

## 11. 🧩 Code Quality & Best Practices

| Kỹ thuật | Mô tả |
|---|---|
| **Lombok** | `@Getter`, `@Slf4j`, `@RequiredArgsConstructor`, `@Builder`, `@AllArgsConstructor(access=PRIVATE)` |
| **MapStruct 1.5.5** | Compile-time DTO ↔ Domain Object mapping — zero reflection overhead |
| **Factory Methods (DDD)** | Static `create()` methods thay vì public constructor — enforce business rules ngay khi tạo object |
| **Value Objects** | `Email`, `PhoneNumber`, `UserId`, `MessageId`, `ConversationId` — immutable, self-validating |
| **Rich Domain Model** | Business logic nằm trong domain object (`markAsDelivered()`, `markAsRead()`, `delete()`, `isParticipant()`) |
| **Soft Delete** | `deleted` flag + `deletedAt` timestamp trước khi hard delete |
| **Optimistic Business Rules** | 1-hour delete window, status transition guard (`SENT→DELIVERED→READ`) |
| **Apache Commons Lang3** | `StringUtils`, `Validate` — utility methods |
| **Google Guava** | Collections, caching utilities |
| **Jackson 2.16** | JSON serialization với `JavaTimeModule` cho `Instant` |
| **Jakarta Validation 3.0** | Bean validation với `@Valid`, `@NotNull`, `@Size`, custom validators |
| **Pagination** | `PageResponse<T>` wrapper ở common-lib — consistent pagination response |

---

## 12. 🧪 Testing Strategy

| Kỹ thuật | Mô tả |
|---|---|
| **JUnit 5 (Jupiter)** | Unit testing framework |
| **Mockito 5.8** | Mocking dependencies trong unit tests |
| **Testcontainers 1.19** | Integration tests với real Docker containers (PostgreSQL, MongoDB, Redis, RabbitMQ) |
| **REST Assured 5.4** | HTTP integration testing cho REST APIs |
| **Maven Surefire** | Unit test runner (`**/*Test.java`, `**/*Tests.java`) |
| **Maven Failsafe** | Integration test runner (`**/*IT.java`, `**/*IntegrationTest.java`) |
| **Test Profiles** | Separate config cho test environment |

---

## 13. 🔄 CI/CD & DevOps

| Kỹ thuật | Mô tả |
|---|---|
| **GitHub Actions** | Workflows: `build.yml`, `test.yml`, `deploy.yml` |
| **Maven Build Lifecycle** | `mvn clean install` → build tất cả modules cùng lúc |
| **Multi-Module Build** | Parent POM quản lý build order, dependency versions tập trung |
| **Docker Image Building** | Build image từ multi-stage Dockerfile: Maven build stage → Runtime stage |
| **Environment-based Config** | `.env` file cho local, env vars cho Docker/Kubernetes |
| **Shell Scripts** | `deploy.sh`, `health-check.sh`, `start.sh`, `stop.sh`, `clean.sh`, `logs.sh` — DevOps automation |
| **Maven Profiles** | `dev` (default), `prod`, `skip-tests` |

---

## 14. 💡 Design Patterns Applied

| Pattern | Nơi áp dụng |
|---|---|
| **Aggregate Root** | `User`, `Message`, `Conversation` — kiểm soát toàn bộ invariants |
| **Repository Pattern** | `UserRepository`, `MessageRepository` — abstract data access |
| **Factory Method** | `User.create()`, `Message.create()`, `Conversation.create()` |
| **Value Object** | `Email`, `PhoneNumber`, `UserId`, `MessageId`, `MessageContent` |
| **Event Publisher** | `MessageEventPublisher`, `UserEventPublisher` |
| **Strategy Pattern** | Delivery strategy: online (WebSocket) vs offline (Redis inbox + FCM) |
| **Template Method** | `BaseException → BusinessException / ResourceNotFoundException` |
| **Builder Pattern** | Lombok `@Builder`, FCM `Message.builder()` |
| **Singleton (Spring Bean)** | Services, Configs là Spring-managed singletons |
| **Filter Chain** | `LoggingFilter → AuthenticationFilter → RateLimiterFilter → CircuitBreakerFilter` tại Gateway |

---

## 15. 📐 Scalability Patterns

| Pattern | Mô tả |
|---|---|
| **Horizontal Scaling** | Stateless services scale bằng `--scale` (Docker Compose) hoặc HPA (Kubernetes) |
| **Stateful Session Affinity** | Chat Service dùng sticky sessions (Nginx consistent hash) vì WebSocket là stateful |
| **Redis as Shared State** | Cross-instance state (connection registry, pub/sub) để scale Chat Service |
| **Read Replica** | PostgreSQL read replicas cho query-heavy workloads |
| **Database Sharding** | MongoDB shard by `conversationId`, PostgreSQL partition by `user_id` |
| **Asynchronous Decoupling** | RabbitMQ buffer hấp thụ traffic spikes, tránh cascade failure |
| **Cache-Aside Pattern** | Redis miss → query DB → populate cache |
| **TTL-Based Eviction** | Không cần manual cache invalidation cho time-sensitive data |

---

## 📋 CV Summary (Ghi vào CV)

```
Designed & built a production-ready WhatsApp Clone using Microservices Architecture:
• Java 17 / Spring Boot 3.2 / Spring Cloud Gateway — 6 microservices, 16 Docker containers
• Domain-Driven Design (DDD): Aggregate Roots, Value Objects, Hexagonal Architecture
• Real-time messaging with WebSocket (STOMP) — designed for 2M concurrent connections
• Dual database: PostgreSQL (metadata) + MongoDB (messages, 50TB, TTL index) + Redis (cache)
• Event-driven with RabbitMQ: user.events / message.events / notification.events exchanges
• Push notifications: Firebase FCM (Android/Web) + APNs (iOS), multi-device support
• Security: JWT (JJWT), BCrypt, Stateless auth, RBAC, Rate limiting (Bucket4j Token Bucket)
• Resilience: Circuit Breaker (Resilience4j), Retry with exponential backoff
• Observability: Prometheus + Grafana + ELK Stack (Elasticsearch / Logstash / Kibana)
• Scalability: Designed for 100M DAU, 230K RPS peak — horizontal scaling patterns documented
• DevOps: Docker Compose orchestration, Nginx reverse proxy, GitHub Actions CI/CD
```

---

*Generated from codebase analysis — February 2026*


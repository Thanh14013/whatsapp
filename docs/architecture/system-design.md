# WhatsApp Clone – System Design

> **Scale target**: 100 million DAU · 20 messages / user / day · 230 K RPS peak

---

## Table of Contents

1. [Requirements](#1-requirements)
2. [Capacity Estimation](#2-capacity-estimation)
3. [High-Level Architecture](#3-high-level-architecture)
4. [Service Breakdown](#4-service-breakdown)
5. [Database Design](#5-database-design)
6. [Message Flow](#6-message-flow)
7. [Real-Time Communication](#7-real-time-communication)
8. [Event-Driven Architecture](#8-event-driven-architecture)
9. [Caching Strategy](#9-caching-strategy)
10. [Notification System](#10-notification-system)
11. [Scheduled Jobs & Cleanup](#11-scheduled-jobs--cleanup)
12. [API Gateway](#12-api-gateway)
13. [Observability](#13-observability)
14. [Security](#14-security)
15. [Scalability Patterns](#15-scalability-patterns)
16. [Technology Stack](#16-technology-stack)

---

## 1. Requirements

### Functional Requirements

| # | Requirement |
|---|-------------|
| F1 | Real-time one-to-one messaging |
| F2 | Group conversations |
| F3 | Message delivery status: **SENT → DELIVERED → READ** |
| F4 | Offline message handling – messages delivered on reconnect |
| F5 | Push notifications (FCM / APNs) for offline users |
| F6 | User registration, authentication (JWT), profile management |
| F7 | Online / offline / away / busy presence status |
| F8 | Message history with pagination (up to 90-day retention) |
| F9 | Multi-device support per user |
| F10 | Undelivered message inbox (1-year retention) |

### Non-Functional Requirements

| # | Requirement | Target |
|---|-------------|--------|
| N1 | Availability | 99.9 % uptime |
| N2 | Latency | p99 < 100 ms (message delivery, online users) |
| N3 | Throughput | 230 K RPS peak |
| N4 | Storage | ~50 TB for 4-month average retention |
| N5 | Scalability | Horizontal scaling for all stateless services |
| N6 | Security | JWT auth, BCrypt passwords, TLS everywhere |
| N7 | Observability | Prometheus metrics, structured logging, health checks |

---

## 2. Capacity Estimation

### Traffic

```
DAU                     = 100,000,000
Messages / user / day   = 20
Total messages / day    = 2,000,000,000 (2 B)

Average RPS             = 2,000,000,000 / 86,400 ≈ 23,000
Peak RPS (10× avg)      = 230,000
```

### Storage

```
Avg message size        = 200 bytes  (content + metadata)
Daily storage           = 2 B × 200 B          = 400 GB / day
Monthly storage         = 400 GB × 30          ≈ 12 TB / month
4-month retention       = 12 TB × 4            ≈ 50 TB
```

### WebSocket Connections

```
Concurrent users        = 100M × 10 % (active simultaneously) = 10M
WebSocket connections / machine = 1M
Machines required       ≥ 10 (Chat Service replicas)
```

### Database Connections (HikariCP)

```
Max pool per service    = 10 connections
Service replicas        = 3–10 per service
Max DB connections      = 10 replicas × 10 = 100 per DB
```

---

## 3. High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              Clients                                      │
│               Mobile (Android/iOS)  ·  Web Browser                       │
└──────────────────────────────────┬───────────────────────────────────────┘
                                   │ HTTP / WebSocket
                                   ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                           Nginx  (Port 80/443)                            │
│            Reverse Proxy · TLS Termination · Rate Limiting                │
└──────────────────────────────┬───────────────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                        API Gateway  (Port 8080)                           │
│         JWT Auth · Routing · Rate Limiting · Circuit Breaker              │
└────┬──────────────┬──────────────┬──────────────────────────────────────┘
     │              │              │
     ▼              ▼              ▼
┌────────┐   ┌─────────────┐  ┌────────────────────┐
│  User  │   │    Chat     │  │   Notification     │
│ Service│   │  Service    │  │    Service         │
│  8081  │   │    8082     │  │      8084          │
└───┬────┘   └──────┬──────┘  └────────┬───────────┘
    │               │                  │
    │          ┌────▼──────────────────▼───────┐
    │          │         RabbitMQ              │
    │          │  message.events exchange      │
    │          └────────────────┬──────────────┘
    │                           │
    │                    ┌──────▼────────────┐
    │                    │ Message Processor │
    │                    │     Service       │
    │                    │      8085         │
    │                    └───────────────────┘
    │
    │          ┌──────────────────────────────┐
    └─────────►│     Scheduled Jobs  8086     │
               │  Cleanup · Maintenance       │
               └──────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                    │
│                                                         │
│  PostgreSQL 15      MongoDB 7.0       Redis 7.2         │
│  (User/Conv meta)   (Messages)        (Cache/Sessions)  │
└─────────────────────────────────────────────────────────┘
```

---

## 4. Service Breakdown

### 4.1 API Gateway (Port 8080)

Built on **Spring Cloud Gateway** (reactive / Netty).

| Responsibility | Implementation |
|----------------|---------------|
| JWT validation | `AuthenticationFilter` – HS256, 24-hr expiry |
| Rate limiting | `RequestRateLimiter` + Redis token bucket (Bucket4j) |
| Circuit breaking | Resilience4j `CircuitBreaker` per downstream |
| Retry | 3 retries with exponential back-off |
| CORS | Global CORS config (all origins, configurable) |
| Routing | Path-based → User, Chat, Notification services |
| WebSocket proxy | Transparent pass-through to Chat Service |

**Public endpoints** (no JWT):
- `POST /api/users` – registration
- `GET /actuator/health` – health probe

### 4.2 User Service (Port 8081)

**Domain-Driven Design** – Hexagonal Architecture.

```
interfaces/rest       ←  HTTP adapters  (UserController, UserQueryController)
application/service   ←  Use-case layer (UserApplicationService)
domain/model          ←  Aggregate Root: User, UserProfile, Value Objects
infrastructure        ←  JPA repositories, Redis cache, RabbitMQ publisher
```

**Aggregate Root: `User`**

```java
User {
  UserId         id
  String         username       // unique
  Email          email          // value object, validated
  PhoneNumber    phoneNumber    // E.164, unique
  String         passwordHash   // BCrypt strength 12
  UserProfile    profile        // embedded (displayName, bio, avatarUrl)
  UserStatus     status         // ONLINE | OFFLINE | AWAY | BUSY
  boolean        active
  boolean        emailVerified
  boolean        phoneVerified
  Instant        createdAt
  Instant        updatedAt
}
```

**Cache strategy**: User profile cached in Redis for **1 hour** (key: `user:{id}`).

**Events published** (RabbitMQ `user.events` exchange):
- `user.created`, `user.updated`, `user.deleted`, `user.status.changed`

### 4.3 Chat Service (Port 8082)

Handles real-time messaging and conversation management.

**Dual-database persistence:**
- **PostgreSQL**: Conversation metadata (participants, last message, timestamps)
- **MongoDB**: Message content (immutable documents, indexed by `conversationId + createdAt`)

**WebSocket**: STOMP over raw WebSocket at `/ws/chat`.

**Domain models:**

```
Conversation (PostgreSQL)
  id              – UUID
  type            – ONE_TO_ONE | GROUP
  participants    – List<Participant> (userId, displayName, joinedAt)
  lastMessageId
  lastMessageTimestamp
  active

Message (MongoDB)
  id              – Snowflake ID (sortable, globally unique)
  conversationId
  senderId
  receiverId
  contentType     – TEXT | IMAGE | VIDEO | AUDIO | DOCUMENT
  content
  mediaUrl
  status          – SENT | DELIVERED | READ | FAILED
  sentAt / deliveredAt / readAt
  replyToMessageId
  deleted
```

**Events published** (RabbitMQ `message.events` exchange):
- `message.sent`, `message.delivered`, `message.read`

### 4.4 Message Processor Service (Port 8085)

Stateless background processor. Consumes RabbitMQ events.

**Responsibilities:**

| Job | Description |
|-----|-------------|
| Delivery detection | Checks if recipient is online (Redis `user:status:{id}`) |
| Inbox caching | If offline → caches message ID in Redis inbox (`inbox:{userId}`) |
| Notification trigger | Publishes to Notification Service if recipient offline |
| Online recovery | On user connection event, flushes inbox cache and delivers queued messages |
| Periodic scan | Every 30 s scans Redis for users with pending inbox messages who came online |

### 4.5 Notification Service (Port 8084)

Push notifications via **Firebase Cloud Messaging (FCM)** (Android/Web) and **APNs** (iOS).

**Device token management:**
- Stored in Redis: `device_tokens:{userId}` → Set of token strings
- Multi-device: one user → multiple tokens (mobile + tablet + web)
- Auto-expiry via Redis TTL

**Notification trigger flow:**
1. Message Processor publishes `offline.notification` event
2. Notification Service consumer picks it up
3. Fetches all device tokens for the recipient
4. Sends FCM/APNs push with sender name + message preview
5. Invalid tokens auto-removed (FCM `UNREGISTERED` response)

### 4.6 Scheduled Jobs Service (Port 8086)

Spring `@Scheduled` cron jobs for housekeeping.

| Job | Schedule | Action |
|-----|----------|--------|
| `MessageCleanupJob` | Daily 2 AM | Hard-delete messages older than 90 days from MongoDB |
| `UserPolicyCleanupJob` | Daily 3 AM | Deactivate users inactive > 365 days; purge deleted users |
| `CacheCleanupJob` | Every 6 h | Evict stale Redis keys; refresh warm cache |

---

## 5. Database Design

### 5.1 PostgreSQL – Relational Metadata

```sql
-- Users table
CREATE TABLE users (
    id              VARCHAR(50) PRIMARY KEY,
    username        VARCHAR(30) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    phone_number    VARCHAR(20) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(50),
    bio             VARCHAR(500),
    avatar_url      TEXT,
    status_message  VARCHAR(255),
    status          VARCHAR(20) DEFAULT 'OFFLINE',
    active          BOOLEAN DEFAULT TRUE,
    email_verified  BOOLEAN DEFAULT FALSE,
    phone_verified  BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    last_seen_at    TIMESTAMPTZ
);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email    ON users(email);

-- Conversations table
CREATE TABLE conversations (
    id                      VARCHAR(50) PRIMARY KEY,
    type                    VARCHAR(20) NOT NULL,   -- ONE_TO_ONE | GROUP
    name                    VARCHAR(100),
    description             VARCHAR(500),
    avatar_url              TEXT,
    last_message_id         VARCHAR(50),
    last_message_timestamp  TIMESTAMPTZ,
    active                  BOOLEAN DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL
);

-- Conversation participants
CREATE TABLE conversation_participants (
    conversation_id VARCHAR(50) REFERENCES conversations(id),
    user_id         VARCHAR(50) REFERENCES users(id),
    display_name    VARCHAR(50),
    joined_at       TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (conversation_id, user_id)
);
CREATE INDEX idx_participants_user ON conversation_participants(user_id);
```

### 5.2 MongoDB – Message Store

```js
// Collection: messages
// Shard key: { conversationId: 1, createdAt: 1 }
{
  "_id":              "msg_01HN2K...",   // Snowflake ID
  "conversationId":   "conv_01HN...",
  "senderId":         "usr_01HN...",
  "receiverId":       "usr_02XY...",
  "contentType":      "TEXT",
  "content":          "Hey! How are you?",
  "mediaUrl":         null,
  "status":           "READ",
  "sentAt":           ISODate("2026-02-24T08:30:00Z"),
  "deliveredAt":      ISODate("2026-02-24T08:30:01Z"),
  "readAt":           ISODate("2026-02-24T08:32:00Z"),
  "replyToMessageId": null,
  "deleted":          false,
  "createdAt":        ISODate("2026-02-24T08:30:00Z")
}

// Indexes
db.messages.createIndex({ conversationId: 1, createdAt: -1 })  // pagination
db.messages.createIndex({ senderId: 1, createdAt: -1 })         // sent history
db.messages.createIndex({ "sentAt": 1 }, { expireAfterSeconds: 7776000 })  // 90-day TTL
```

### 5.3 Redis – Caching & Sessions

| Key pattern | Type | TTL | Purpose |
|-------------|------|-----|---------|
| `user:{id}` | Hash | 1 h | User profile cache |
| `user:status:{id}` | String | 5 min | Online presence |
| `inbox:{userId}` | Set | 1 year | Undelivered message IDs |
| `device_tokens:{userId}` | Set | 30 days | Push notification tokens |
| `rate_limit:{ip}:{endpoint}` | String | 60 s | Rate limiter bucket |
| `session:{token}` | String | 24 h | JWT session (for revocation) |

---

## 6. Message Flow

### 6.1 Send to Online Recipient

```
Client A         API Gateway      Chat Service        RabbitMQ          Client B
   │                  │                │                  │                 │
   │─ POST /messages ►│                │                  │                 │
   │                  │─ route ───────►│                  │                 │
   │                  │                │─ persist MongoDB │                 │
   │                  │                │─ update Postgres │                 │
   │                  │                │─ publish ────────►message.sent     │
   │◄─ 201 Created ───│                │                  │                 │
   │                  │                │                  │─ consume ───────►│
   │                  │                │◄──────────────────│── WS deliver ───│
   │◄─ WS delivered ──│◄──────────────►│                  │                 │
```

### 6.2 Send to Offline Recipient

```
Client A         Chat Service       RabbitMQ      Msg Processor   Notif Service
   │                  │                │                │               │
   │─ POST /messages ►│                │                │               │
   │                  │─ persist ──────│                │               │
   │◄─ 201 Created ───│                │                │               │
   │                  │─ publish ──────►message.sent    │               │
   │                  │                │─ consume ──────►               │
   │                  │                │                │─ check status │
   │                  │                │                │  (offline)    │
   │                  │                │                │─ cache inbox  │
   │                  │                │                │  (Redis)      │
   │                  │                │                │─ publish ─────►notification
   │                  │                │                │               │─ FCM/APNs
```

### 6.3 Receive Offline Messages on Reconnect

```
Client B         Chat Service           Redis Cache           MongoDB
   │                  │                      │                    │
   │─ WS connect() ──►│                      │                    │
   │                  │─ set ONLINE ─────────►user:status:{id}    │
   │                  │─ check inbox ────────►inbox:{userId}      │
   │                  │◄─ messageIds ─────────│                    │
   │                  │                                            │
   │                  │─ fetch messages ──────────────────────────►│
   │◄─ WS deliver ────│◄──────────────────────────────────────────│
   │─ ACK delivered ─►│                      │                    │
   │                  │─ clear inbox ────────►inbox:{userId}      │
   │                  │─ update status ───────────────────────────►│
```

---

## 7. Real-Time Communication

### WebSocket Protocol

- **Endpoint**: `ws://host:8082/ws/chat?userId={id}&token={jwt}`
- **Protocol**: STOMP over raw WebSocket (or raw JSON frames)
- **Heartbeat**: client sends `ping` every 30 s; server replies `pong`

### Connection Lifecycle

```
1. Client opens WebSocket with JWT in query string
2. Server validates JWT → extracts userId
3. Server updates Redis: user:status:{userId} = ONLINE (TTL 5 min)
4. Server flushes Redis inbox → delivers any pending messages
5. [messaging session active]
6. On disconnect → Redis: user:status:{userId} = OFFLINE
```

### Message Frame Contracts

**Client → Server:**
```json
{ "action": "send_message",    "receiverId": "...", "content": "...", "contentType": "TEXT" }
{ "action": "message_delivered","messageId": "...", "receiverId": "..." }
{ "action": "message_read",    "messageId": "...", "receiverId": "..." }
{ "action": "ping" }
```

**Server → Client:**
```json
{ "type": "incoming_message",  "message": { /* MessageDto */ } }
{ "type": "message_delivered", "messageId": "...", "deliveredAt": "..." }
{ "type": "message_read",      "messageId": "...", "readAt": "..." }
{ "type": "user_status",       "userId": "...",   "status": "ONLINE|OFFLINE" }
{ "type": "pong" }
```

### Horizontal Scaling for WebSocket

Scaling Chat Service beyond a single instance requires **session affinity**:

1. **Nginx sticky sessions** – route same user to same Chat Service instance
2. **Redis Pub/Sub** – inter-instance message broadcast (a message received by instance A is published to Redis channel; all instances subscribe and forward to their locally-connected recipients)
3. **Connection registry** – Redis stores `userId → instanceId` mapping for targeted routing

---

## 8. Event-Driven Architecture

### RabbitMQ Exchanges & Queues

```
Direct exchange: user.events
  ├── queue: user.created
  ├── queue: user.updated
  ├── queue: user.deleted
  └── queue: user.status.changed

Direct exchange: message.events
  ├── queue: message.sent         ← consumed by Message Processor
  ├── queue: message.delivered    ← consumed by Chat Service (status update)
  └── queue: message.read         ← consumed by Chat Service (status update)

Direct exchange: notification.events
  └── queue: offline.notification ← consumed by Notification Service
```

### Event Flow

```
User Service ──────────────────► user.events
Chat Service ──────────────────► message.events
Message Processor ─────────────► notification.events
                                       │
                              Notification Service
                                       │
                                  FCM / APNs
```

### Why RabbitMQ?

| Concern | Solution |
|---------|----------|
| Decoupling | Sender and consumer are independent |
| Resilience | Messages survive service restarts (durable queues) |
| Backpressure | Consumer controls rate; no message loss |
| Fan-out | Multiple consumers can listen to the same exchange |

---

## 9. Caching Strategy

### Multi-Level Cache

```
Client Request
      │
      ▼
 Redis L1 Cache (milliseconds)
      │ miss
      ▼
 PostgreSQL / MongoDB (milliseconds to seconds)
      │
      ▼
 Populate Redis (write-through / write-behind)
```

### Cache Policies

| Data | Strategy | TTL | Invalidation |
|------|----------|-----|--------------|
| User profile | Write-through | 1 h | On profile update |
| User status | Write-through | 5 min | On WebSocket connect/disconnect |
| Inbox messages | Write-on-offline | 1 year | On delivery ACK |
| Device tokens | Write-through | 30 days | On token removal |
| Rate-limit buckets | Write-through | 60 s | Auto-expire |

### Redis Data Structures

- **Hash** for user profiles (field-level access)
- **Set** for device tokens (no duplicates)
- **Set** for inbox message IDs (efficient membership check)
- **String + TTL** for user status and rate-limit counters

---

## 10. Notification System

### Push Flow

```
Offline Recipient Detected
         │
         ▼
Message Processor publishes to notification.events
         │
         ▼
Notification Service Consumer
         │
         ├─► Fetch device tokens from Redis
         │        (device_tokens:{userId})
         │
         ├─► Send FCM message (Android / Web)
         │        payload: { sender, message preview, conversationId }
         │
         └─► Send APNs notification (iOS)
                  payload: { alert: { title, body }, badge, sound }
```

### Multi-Device

Each user may have multiple registered tokens (phone + tablet + web).  
All tokens are stored in a Redis **Set**; the Notification Service iterates and sends to all.

Invalid tokens (FCM `UNREGISTERED` / `INVALID_REGISTRATION`) are auto-removed.

---

## 11. Scheduled Jobs & Cleanup

### Retention Policies

| Data | Retention | Cleanup job |
|------|-----------|-------------|
| Messages (MongoDB) | 90 days (configurable) | `MessageCleanupJob` (daily 2 AM) |
| Inbox cache (Redis) | 1 year | `CacheCleanupJob` (every 6 h) |
| User accounts | 365 days after last login | `UserPolicyCleanupJob` (daily 3 AM) |

### Cleanup Safety

- All jobs use **soft operations** first (mark deleted), then hard-purge after a grace period
- Jobs log counts of affected records for auditability
- Failures are caught, logged, and retried on the next schedule (not crash-looped)

---

## 12. API Gateway

### Request Pipeline

```
Incoming Request
       │
       ▼
LoggingFilter          ─ request ID, timing
       │
       ▼
AuthenticationFilter   ─ extract & validate JWT
       │                 inject X-User-Id, X-Username, X-User-Roles headers
       ▼
RateLimiterFilter      ─ Redis token bucket (10 RPS default, 20 burst)
       │
       ▼
CircuitBreakerFilter   ─ open after 50 % failures in 10-s window
       │
       ▼
RetryFilter            ─ 3 retries, exponential back-off
       │
       ▼
Route to downstream service
```

### Route Table

| Route ID | Predicate | Upstream |
|----------|-----------|----------|
| `user-service` | `/api/users/**`, `/api/auth/**` | `http://user-service:8081` |
| `chat-service-rest` | `/api/messages/**`, `/api/conversations/**` | `http://chat-service:8082` |
| `chat-service-ws` | `/ws/**` | `ws://chat-service:8082` |
| `notification-service` | `/notifications/**` | `http://notification-service:8084` |

---

## 13. Observability

### Metrics (Prometheus + Grafana)

All services expose `/actuator/prometheus`.  
Prometheus scrapes every **15 seconds**.

**Key metrics per service:**

| Category | Metric | Description |
|----------|--------|-------------|
| HTTP | `http_server_requests_seconds` | Request rate, latency histogram |
| JVM | `jvm_memory_used_bytes` | Heap / non-heap usage |
| DB | `hikaricp_connections_active` | JDBC connection pool |
| MQ | `spring_rabbitmq_listener_seconds` | Consumer latency |
| Cache | `cache_gets_total{result="hit\|miss"}` | Redis hit rate |
| WS | `websocket_connections_active` | Live WebSocket sessions |

**Recommended Grafana Dashboards:**
- Spring Boot Statistics (ID: 10280)
- JVM Micrometer (ID: 4701)
- RabbitMQ Overview (ID: 10991)

### Logging (ELK Stack)

```
Service → Logstash (TCP :5000) → Elasticsearch → Kibana
```

All services use structured JSON logging:

```json
{
  "timestamp": "2026-02-24T08:30:00Z",
  "level": "INFO",
  "service": "chat-service",
  "traceId": "abc123",
  "userId": "usr_01HN...",
  "message": "Message sent: msg_05ABCXYZ"
}
```

**Kibana index pattern**: `whatsapp-*`

### Health Checks

Spring Boot Actuator `health` endpoints check:
- Database connectivity
- Redis ping
- RabbitMQ diagnostics
- Disk space

Used by Docker health checks and Kubernetes liveness/readiness probes.

---

## 14. Security

### Authentication & Authorisation

| Layer | Mechanism |
|-------|-----------|
| Password storage | BCrypt, strength 12 |
| API authentication | JWT (HS256), 24-h expiry |
| Token transport | `Authorization: Bearer <token>` |
| Downstream propagation | Gateway injects `X-User-Id` header |
| Authorisation | Service-level checks (e.g. user can only update own profile) |

### Rate Limiting

| Scope | Limit | Implementation |
|-------|-------|---------------|
| Per IP (Nginx) | 100 req/s API, 10 req/s auth | `limit_req_zone` |
| Per user (Gateway) | 10 RPS burst 20 | Bucket4j + Redis |
| Account lockout | 5 failed logins → 15-min lockout | Application logic |

### Data Protection

- TLS/SSL termination at Nginx
- All inter-service communication on private Docker network
- Secrets managed via environment variables (Docker secrets in prod)
- MongoDB and PostgreSQL credentials not exposed outside Docker network

---

## 15. Scalability Patterns

### Stateless Services (easy to scale)

Scale with `docker-compose --scale` or Kubernetes HPA:
- API Gateway
- User Service
- Message Processor
- Notification Service
- Scheduled Jobs

### Stateful Service (Chat – WebSocket)

Requires coordination:
1. **Sticky sessions** at Nginx (consistent hash by `userId`)
2. **Redis Pub/Sub** for cross-instance message forwarding
3. **Connection registry** (`user:instance:{userId}` in Redis)

### Database Scaling

**PostgreSQL:**
- Read replicas for `GET` queries
- Partition `users` and `conversations` by `id` range
- HikariCP connection pooling (max 10 per replica)

**MongoDB:**
- Shard key: `{ conversationId: 1, createdAt: 1 }`
- Replica set: 1 primary + 2 secondaries
- TTL index for automatic 90-day cleanup

**Redis:**
- Separate Redis instances per service (to avoid noisy neighbours)
- Master-replica replication
- Redis Cluster for key distribution at scale

---

## 16. Technology Stack

### Backend

| Technology | Version | Role |
|------------|---------|------|
| Java | 17 | Primary language |
| Spring Boot | 3.2.2 | Application framework |
| Spring Cloud Gateway | 2023.0.0 | Reactive API Gateway |
| Spring Data JPA | 3.2.x | PostgreSQL ORM |
| Spring Data MongoDB | 4.2.x | MongoDB integration |
| Spring AMQP | 3.1.x | RabbitMQ messaging |
| Spring WebSocket | 6.1.x | Real-time communication |
| Resilience4j | 2.x | Circuit breaker |
| Bucket4j | 8.x | Rate limiting |
| JJWT | 0.12.5 | JWT auth |
| MapStruct | 1.5.5 | DTO mapping |
| Lombok | 1.18.x | Boilerplate reduction |
| Flyway | 9.x | DB migrations |
| Micrometer | 1.12.x | Metrics |

### Infrastructure

| Technology | Version | Role |
|------------|---------|------|
| PostgreSQL | 15 | Relational metadata |
| MongoDB | 7.0 | Message storage |
| Redis | 7.2 | Cache & sessions |
| RabbitMQ | 3.12 | Event bus |
| Firebase Admin SDK | 9.2.0 | FCM push notifications |
| Docker | 24.0 | Containerisation |
| Docker Compose | 2.23 | Local orchestration |
| Nginx | latest (alpine) | Reverse proxy |
| Prometheus | latest | Metrics collection |
| Grafana | latest | Metrics visualisation |
| Elasticsearch | 8.11 | Log storage |
| Logstash | 8.11 | Log processing |
| Kibana | 8.11 | Log visualisation |

---

*Last updated: February 2026 | Version: 1.0.0*


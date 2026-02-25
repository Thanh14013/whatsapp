# 💬 Chat Service — WhatsApp Clone

> **Microservice** chịu trách nhiệm xử lý toàn bộ luồng **nhắn tin thời gian thực**, quản lý cuộc hội thoại và theo dõi trạng thái tin nhắn trong hệ thống WhatsApp Clone.

---

## 📑 Mục lục

- [Tổng quan](#-tổng-quan)
- [Tính năng chính](#-tính-năng-chính)
- [Kiến trúc](#-kiến-trúc)
- [Tech Stack](#-tech-stack)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Domain Model](#-domain-model)
- [REST API](#-rest-api)
- [WebSocket API](#-websocket-api)
- [Message Queue (RabbitMQ)](#-message-queue-rabbitmq)
- [Database Schema](#-database-schema)
- [Caching Strategy](#-caching-strategy)
- [Cấu hình](#-cấu-hình)
- [Chạy ứng dụng](#-chạy-ứng-dụng)
- [Docker](#-docker)
- [Monitoring & Observability](#-monitoring--observability)
- [Business Rules](#-business-rules)

---

## 🌐 Tổng quan

**Chat Service** là một microservice độc lập trong hệ thống WhatsApp Clone, chạy trên cổng **8082**. Service này được thiết kế theo kiến trúc **Domain-Driven Design (DDD)** với bốn layer rõ ràng: Domain, Application, Infrastructure và Interface.

Service sử dụng **chiến lược dual-database**:
- **PostgreSQL** — lưu trữ metadata cuộc hội thoại, danh sách thành viên, trạng thái giao nhận (ACID compliance)
- **MongoDB** — lưu trữ nội dung tin nhắn (scalable cho lượng lớn dữ liệu)
- **Redis** — caching cuộc hội thoại, danh sách online users, inbox undelivered messages

---

## ✨ Tính năng chính

| Tính năng | Mô tả |
|---|---|
| 📨 Gửi tin nhắn | Gửi tin nhắn văn bản, hình ảnh, video, audio, tài liệu |
| 🗨️ Cuộc hội thoại | Tạo và quản lý hội thoại 1-1 và nhóm |
| ✅ Trạng thái tin nhắn | Theo dõi vòng đời: `SENT → DELIVERED → READ` |
| 🔴 Tin nhắn chưa giao | Lưu trữ và giao lại tin nhắn khi người dùng online |
| 🔌 WebSocket (STOMP) | Kết nối thời gian thực qua endpoint `/ws/chat` |
| 📬 Event-driven | Publish/consume sự kiện qua RabbitMQ |
| 🗑️ Xóa tin nhắn | Soft-delete trong vòng 1 giờ kể từ khi gửi |
| 📄 Lịch sử chat | Phân trang lịch sử hội thoại |
| 👥 Quản lý nhóm | Thêm/xóa thành viên, phân quyền admin |
| 📊 Metrics | Tích hợp Prometheus/Micrometer |

---

## 🏛️ Kiến trúc

```
┌─────────────────────────────────────────────────────────────────┐
│                        Chat Service                             │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  Interface Layer                         │   │
│  │   REST Controllers  │  WebSocket Controllers            │   │
│  │   (ConversationController, MessageController)           │   │
│  └────────────────────────┬────────────────────────────────┘   │
│                           │                                     │
│  ┌────────────────────────▼────────────────────────────────┐   │
│  │                 Application Layer                        │   │
│  │   ChatApplicationService  │  ConversationService        │   │
│  │   Use Cases: Send, Deliver, Read, GetHistory            │   │
│  └────────────────────────┬────────────────────────────────┘   │
│                           │                                     │
│  ┌────────────────────────▼────────────────────────────────┐   │
│  │                   Domain Layer                           │   │
│  │   Message (Aggregate Root)  │  Conversation (AR)        │   │
│  │   MessageDomainService      │  Domain Repositories      │   │
│  │   Value Objects: MessageContent, Participant, ...        │   │
│  └────────────────────────┬────────────────────────────────┘   │
│                           │                                     │
│  ┌────────────────────────▼────────────────────────────────┐   │
│  │               Infrastructure Layer                       │   │
│  │  ┌──────────┐ ┌─────────┐ ┌───────┐ ┌──────────────┐  │   │
│  │  │PostgreSQL│ │ MongoDB │ │ Redis │ │   RabbitMQ   │  │   │
│  │  │(metadata)│ │(messages│ │(cache)│ │  (events)    │  │   │
│  │  └──────────┘ └─────────┘ └───────┘ └──────────────┘  │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Luồng gửi tin nhắn

```
Client ──POST /api/v1/messages──► MessageController
                                         │
                              ChatApplicationService
                                         │
                          ┌──────────────┼──────────────────┐
                          │              │                   │
                  Validate Conv    MessageDomainService   Publish Event
                  (PostgreSQL)     .createMessage()      (RabbitMQ)
                                         │
                               MessageRepository.save()
                                    (MongoDB)
                                         │
                              Update Conversation
                               (PostgreSQL)
                                         │
                             Evict Cache (Redis)
```

---

## 🛠️ Tech Stack

| Thành phần | Công nghệ | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.x |
| Build Tool | Maven | 3.9+ |
| ORM / JPA | Spring Data JPA + Hibernate | - |
| NoSQL | Spring Data MongoDB | - |
| Cache | Spring Data Redis + Lettuce | - |
| Message Broker | Spring AMQP (RabbitMQ) | - |
| WebSocket | Spring WebSocket + STOMP + SockJS | - |
| Database Migration | Flyway | - |
| Mapping | MapStruct | - |
| Boilerplate | Lombok | - |
| ID Generation | Hutool Snowflake (`cn.hutool:hutool-all:5.8.24`) | - |
| Monitoring | Micrometer + Prometheus | - |
| Testing | JUnit 5, Testcontainers, H2, Embedded MongoDB | - |
| Container | Docker (Multi-stage, JRE Alpine) | - |

---

## 📁 Cấu trúc dự án

```
chat/
├── src/main/java/com/whatsapp/chat/
│   │
│   ├── ChatServiceApplication.java          # Entry point
│   │
│   ├── domain/                              # 💡 DOMAIN LAYER (Business Logic)
│   │   ├── model/
│   │   │   ├── Message.java                 # Aggregate Root
│   │   │   ├── Conversation.java            # Aggregate Root
│   │   │   ├── MessageStatus.java           # Enum: SENT, DELIVERED, READ
│   │   │   ├── MessageType.java             # Enum: TEXT, IMAGE, VIDEO, ...
│   │   │   ├── ConversationType.java        # Enum: ONE_TO_ONE, GROUP, BROADCAST
│   │   │   └── vo/
│   │   │       ├── MessageId.java           # Value Object
│   │   │       ├── ConversationId.java      # Value Object
│   │   │       ├── MessageContent.java      # Value Object (text/media)
│   │   │       └── Participant.java         # Value Object
│   │   ├── repository/
│   │   │   ├── MessageRepository.java       # Repository interface
│   │   │   └── ConversationRepository.java  # Repository interface
│   │   └── service/
│   │       ├── MessageDomainService.java    # Domain Service
│   │       └── DeliveryTrackingService.java # Domain Service
│   │
│   ├── application/                         # 🔧 APPLICATION LAYER (Use Cases)
│   │   ├── dto/
│   │   │   ├── MessageDto.java
│   │   │   ├── ConversationDto.java
│   │   │   ├── SendMessageRequest.java
│   │   │   ├── CreateConversationRequest.java
│   │   │   ├── MessageDeliveredRequest.java
│   │   │   └── MessageReceivedResponse.java
│   │   ├── mapper/
│   │   │   ├── MessageMapper.java           # MapStruct mapper
│   │   │   └── ConversationMapper.java      # MapStruct mapper
│   │   ├── service/
│   │   │   ├── ChatApplicationService.java  # Orchestrates messaging use cases
│   │   │   ├── ConversationService.java     # Manages conversations
│   │   │   └── MessageQueryService.java     # Query-side service
│   │   └── usecase/
│   │       ├── SendMessageUseCase.java
│   │       ├── DeliverMessageUseCase.java
│   │       └── GetChatHistoryUseCase.java
│   │
│   ├── infrastructure/                      # ⚙️ INFRASTRUCTURE LAYER
│   │   ├── config/
│   │   │   ├── RabbitMQConfig.java          # Exchanges, Queues, Bindings
│   │   │   ├── WebSocketConfig.java         # STOMP + SockJS config
│   │   │   ├── RedisConfig.java
│   │   │   ├── MongoConfig.java
│   │   │   └── PostgresConfig.java
│   │   ├── persistence/
│   │   │   ├── mongodb/
│   │   │   │   ├── document/MessageDocument.java        # MongoDB document
│   │   │   │   └── repository/MessageMongoRepository.java
│   │   │   └── postgres/
│   │   │       ├── entity/ConversationEntity.java       # JPA entity
│   │   │       └── repository/
│   │   ├── cache/
│   │   │   ├── InboxCacheService.java       # Redis cache management
│   │   │   └── UndeliveredMessageCache.java
│   │   ├── messaging/
│   │   │   ├── MessageEventPublisher.java   # Publish to RabbitMQ
│   │   │   ├── consumer/MessageQueueConsumer.java
│   │   │   └── producer/MessageQueueProducer.java
│   │   ├── websocket/
│   │   │   ├── WebSocketHandler.java
│   │   │   ├── WebSocketSessionManager.java
│   │   │   └── ConnectionRegistry.java
│   │   └── idgen/
│   │       └── SnowflakeIdGenerator.java    # Snowflake ID generation
│   │
│   └── interfaces/                          # 🌐 INTERFACE LAYER (Entry Points)
│       ├── rest/
│       │   ├── MessageController.java       # /api/v1/messages
│       │   └── ConversationController.java  # /api/v1/conversations
│       ├── websocket/
│       │   ├── ChatWebSocketController.java
│       │   └── MessageEventHandler.java
│       └── exception/
│           └── ChatExceptionHandler.java    # Global exception handler
│
├── src/main/resources/
│   ├── application.yaml                     # Main configuration
│   ├── application-prod.yml                 # Production configuration
│   └── db/migration/
│       ├── V1__Create_conversations_table.sql
│       ├── V2__Create_conversation_participants_table.sql
│       └── V3__Create_message_delivery_tracking_table.sql
│
├── Dockerfile                               # Multi-stage Docker build
└── pom.xml
```

---

## 🧩 Domain Model

### Message (Aggregate Root)

```
Message
├── id               : MessageId (Snowflake)
├── conversationId   : ConversationId
├── senderId         : String
├── receiverId       : String
├── content          : MessageContent (text | imageUrl | videoUrl | ...)
├── status           : MessageStatus (SENT → DELIVERED → READ)
├── deleted          : boolean (soft-delete)
├── createdAt        : Instant
├── deliveredAt      : Instant
├── readAt           : Instant
└── deletedAt        : Instant
```

**Hành vi Domain:**
- `Message.create(...)` — factory method, validate sender ≠ receiver
- `markAsDelivered()` — chuyển trạng thái SENT → DELIVERED
- `markAsRead()` — chuyển trạng thái → READ, tự động set deliveredAt nếu chưa có
- `delete(userId)` — soft-delete, chỉ sender được xóa, trong vòng 1 giờ sau khi gửi

### Conversation (Aggregate Root)

```
Conversation
├── id                  : ConversationId (Snowflake)
├── type                : ConversationType (ONE_TO_ONE | GROUP | BROADCAST)
├── name                : String (cho group)
├── participants        : List<Participant>
├── lastMessageId       : String
├── lastMessageContent  : String
├── lastMessageAt       : Instant
├── createdAt           : Instant
└── updatedAt           : Instant
```

**Hành vi Domain:**
- `Conversation.createOneToOne(...)` — tạo hội thoại 1-1
- `Conversation.createGroup(...)` — tạo nhóm, creator được set là admin
- `isParticipant(userId)` — kiểm tra tư cách thành viên
- `updateLastMessage(...)` — cập nhật tin nhắn cuối
- Participant key: participants được sort alphabetically để đảm bảo uniqueness

### Value Objects

| VO | Mô tả |
|---|---|
| `MessageId` | Wrapper Snowflake ID cho message |
| `ConversationId` | Wrapper Snowflake ID cho conversation |
| `MessageContent` | Immutable, factory: `.text()`, `.image()`, `.video()`, `.audio()`, `.document()` |
| `Participant` | userId + displayName + isAdmin flag |

---

## 🌍 REST API

### Conversations — `/api/v1/conversations`

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/conversations` | Tạo cuộc hội thoại mới (1-1 hoặc nhóm) |
| `GET` | `/api/v1/conversations?userId={id}` | Lấy danh sách hội thoại của user |
| `GET` | `/api/v1/conversations/{id}?userId={id}` | Lấy chi tiết một hội thoại |
| `PUT` | `/api/v1/conversations/{id}/read?userId={id}` | Đánh dấu đã đọc |
| `POST` | `/api/v1/conversations/{id}/participants` | Thêm thành viên vào nhóm |
| `DELETE` | `/api/v1/conversations/{id}/participants/{userId}` | Xóa thành viên khỏi nhóm |

#### Ví dụ — Tạo hội thoại 1-1

```http
POST /api/v1/conversations
Content-Type: application/json

{
  "type": "ONE_TO_ONE",
  "participant1Id": "user-001",
  "participant1Name": "Alice",
  "participant2Id": "user-002",
  "participant2Name": "Bob"
}
```

#### Ví dụ — Tạo nhóm chat

```http
POST /api/v1/conversations
Content-Type: application/json

{
  "type": "GROUP",
  "name": "Team Alpha",
  "description": "Dev team chat",
  "creatorId": "user-001",
  "creatorName": "Alice",
  "additionalParticipants": [
    { "userId": "user-002", "displayName": "Bob" },
    { "userId": "user-003", "displayName": "Charlie" }
  ]
}
```

---

### Messages — `/api/v1/messages`

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/messages` | Gửi tin nhắn mới |
| `GET` | `/api/v1/messages/{id}` | Lấy thông tin một tin nhắn |
| `GET` | `/api/v1/messages/conversation/{conversationId}?page=0&size=50` | Lấy lịch sử chat (phân trang) |
| `PUT` | `/api/v1/messages/{id}/delivered?userId={id}` | Đánh dấu đã giao |
| `PUT` | `/api/v1/messages/{id}/read?userId={id}` | Đánh dấu đã đọc |
| `DELETE` | `/api/v1/messages/{id}?userId={id}` | Xóa tin nhắn (soft-delete, trong 1 giờ) |

#### Ví dụ — Gửi tin nhắn

```http
POST /api/v1/messages
Content-Type: application/json

{
  "receiverId": "user-002",
  "content": "Hello World!",
  "contentType": "TEXT"
}
```

#### Cấu trúc `MessageDto` (response)

```json
{
  "id": "1234567890",
  "conversationId": "9876543210",
  "senderId": "user-001",
  "receiverId": "user-002",
  "contentType": "TEXT",
  "content": "Hello World!",
  "mediaUrl": null,
  "status": "SENT",
  "sentAt": "2026-02-25T10:00:00Z",
  "deliveredAt": null,
  "readAt": null,
  "replyToMessageId": null,
  "deleted": false,
  "createdAt": "2026-02-25T10:00:00Z"
}
```

#### Content Types được hỗ trợ

```
TEXT | IMAGE | VIDEO | AUDIO | DOCUMENT
```

---

## 🔌 WebSocket API

Service sử dụng **STOMP over SockJS** cho real-time messaging.

### Kết nối

```
ws://localhost:8082/ws/chat
```

Hỗ trợ fallback qua SockJS cho các môi trường không hỗ trợ WebSocket thuần.

### STOMP Destinations

| Destination | Loại | Mô tả |
|---|---|---|
| `/app/...` | Gửi từ client | Application destination prefix |
| `/topic/...` | Subscribe | Broadcast (nhóm) |
| `/queue/...` | Subscribe | Queue (1-1) |
| `/user/...` | Subscribe | Per-user destination |

### Ví dụ kết nối (JavaScript)

```javascript
const socket = new SockJS('http://localhost:8082/ws/chat');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
  // Subscribe nhận tin nhắn mới
  stompClient.subscribe('/user/queue/messages', function (message) {
    const msg = JSON.parse(message.body);
    console.log('New message:', msg);
  });

  // Subscribe cập nhật trạng thái
  stompClient.subscribe('/user/queue/status', function (message) {
    const status = JSON.parse(message.body);
    console.log('Status update:', status);
  });
});
```

**Cấu hình WebSocket (application.yaml):**
```yaml
websocket:
  endpoint: /ws/chat
  allowed-origins:
    - http://localhost:3000
    - http://localhost:4200
  max-sessions-per-user: 5
  heartbeat-interval: 30000   # 30 giây
  session-timeout: 300000     # 5 phút
```

---

## 📬 Message Queue (RabbitMQ)

### Exchanges & Queues

```
Exchange: message.exchange (Topic Exchange)
│
├── Routing Key: message.sent      ──► Queue: message.sent.queue
├── Routing Key: message.delivered ──► Queue: message.delivered.queue
└── Routing Key: message.read      ──► Queue: message.read.queue
```

Tất cả queues đều là **durable** với TTL **24 giờ** (`x-message-ttl: 86400000`).

### Events được publish

| Event | Routing Key | Khi nào |
|---|---|---|
| `MESSAGE_SENT` | `message.sent` | Tin nhắn được gửi thành công |
| `MESSAGE_DELIVERED` | `message.delivered` | Tin nhắn được giao cho người nhận |
| `MESSAGE_READ` | `message.read` | Người nhận đã đọc tin nhắn |

### Cấu trúc Event Payload

```json
{
  "eventType": "MESSAGE_SENT",
  "messageId": "1234567890",
  "conversationId": "9876543210",
  "senderId": "user-001",
  "receiverId": "user-002",
  "timestamp": "2026-02-25T10:00:00Z"
}
```

**Retry Policy:**
```yaml
rabbitmq:
  listener:
    simple:
      retry:
        enabled: true
        max-attempts: 3
        initial-interval: 1000ms
        multiplier: 2.0
        max-interval: 10000ms
```

---

## 🗄️ Database Schema

### PostgreSQL

**Bảng `conversations`** — Metadata hội thoại

```sql
CREATE TABLE conversations (
    id                     VARCHAR(255) PRIMARY KEY,
    type                   VARCHAR(20) NOT NULL,      -- ONE_TO_ONE | GROUP | BROADCAST
    name                   VARCHAR(100),
    description            TEXT,
    avatar_url             VARCHAR(500),
    last_message_id        VARCHAR(255),
    last_message_timestamp TIMESTAMP,
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Bảng `conversation_participants`** — Thành viên hội thoại

```sql
CREATE TABLE conversation_participants (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    is_admin        BOOLEAN NOT NULL DEFAULT FALSE,
    unread_count    INTEGER NOT NULL DEFAULT 0,
    joined_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at         TIMESTAMP,
    UNIQUE (conversation_id, user_id)
);
```

**Bảng `message_delivery_tracking`** — Tracking giao nhận

```sql
CREATE TABLE message_delivery_tracking (
    id              BIGSERIAL PRIMARY KEY,
    message_id      VARCHAR(255) NOT NULL UNIQUE,
    conversation_id VARCHAR(255) NOT NULL,
    sender_id       VARCHAR(255) NOT NULL,
    receiver_id     VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL,   -- SENT | DELIVERED | READ
    sent_at         TIMESTAMP NOT NULL,
    delivered_at    TIMESTAMP,
    read_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### MongoDB

**Collection `messages`** — Nội dung tin nhắn

```json
{
  "_id": "1234567890",
  "conversationId": "9876543210",
  "senderId": "user-001",
  "receiverId": "user-002",
  "contentText": "Hello World!",
  "contentType": "TEXT",
  "status": "SENT",
  "deleted": false,
  "createdAt": "2026-02-25T10:00:00Z",
  "deliveredAt": null,
  "readAt": null,
  "deletedAt": null
}
```

**Indexes:**
- `{ conversationId: 1, createdAt: -1 }` — Composite index cho query lịch sử chat
- `{ receiverId: 1, status: 1 }` — Composite index cho query tin nhắn chưa giao
- Single field indexes: `conversationId`, `senderId`, `receiverId`, `status`, `createdAt`

---

## ⚡ Caching Strategy

Service sử dụng **Redis database 1** với các cache sau:

| Cache Key Pattern | TTL | Nội dung |
|---|---|---|
| `conversation:{id}` | 30 phút | Dữ liệu cuộc hội thoại |
| `user:conversations:{userId}` | 30 phút | Set ID các hội thoại của user |
| `online:users` | - | Set các user đang online (ZSet/Set) |

**Cache Eviction:** Cache cuộc hội thoại bị xóa mỗi khi có tin nhắn mới được gửi.

```yaml
app:
  cache:
    message-ttl: 1800000       # 30 phút
    conversation-ttl: 3600000  # 1 giờ
    inbox-ttl: 300000          # 5 phút (undelivered messages)
```

---

## ⚙️ Cấu hình

### `application.yaml` — Các tham số quan trọng

```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/whatsapp
    username: admin
    password: password123

  data:
    mongodb:
      uri: mongodb://localhost:27017/whatsapp
    redis:
      host: localhost
      port: 6379
      database: 1

  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: password123

app:
  message:
    max-text-length: 10000     # Giới hạn ký tự tin nhắn
    max-batch-size: 50         # Số tin nhắn tối đa mỗi lần query

  rabbitmq:
    exchanges:
      message-events: message.events
    queues:
      message-sent: message.sent
      message-delivered: message.delivered
      message-read: message.read
```

### Biến môi trường (cho Docker / Production)

| Biến | Mô tả | Mặc định |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL URL | `jdbc:postgresql://localhost:5432/whatsapp` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | `admin` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | `password123` |
| `SPRING_DATA_MONGODB_URI` | MongoDB URI | `mongodb://localhost:27017/whatsapp` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `SPRING_RABBITMQ_USERNAME` | RabbitMQ username | `admin` |
| `SPRING_RABBITMQ_PASSWORD` | RabbitMQ password | `password123` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `default` |

---

## 🚀 Chạy ứng dụng

### Yêu cầu

- Java 17+
- Maven 3.9+
- PostgreSQL 14+
- MongoDB 6+
- Redis 7+
- RabbitMQ 3.12+

### Chạy local

```powershell
# 1. Clone và di chuyển vào thư mục
cd "D:\system design\Whatsapp\chat"

# 2. Build (bỏ qua test)
mvn clean package -DskipTests

# 3. Chạy ứng dụng
java -jar target/chat-service.jar
```

### Chạy bằng Maven

```powershell
mvn spring-boot:run
```

### Chạy với profile production

```powershell
java -jar target/chat-service.jar --spring.profiles.active=prod
```

### Khởi động các dependencies (Docker Compose)

```yaml
# docker-compose.yml (ví dụ)
services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: whatsapp
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: password123
    ports:
      - "5432:5432"

  mongodb:
    image: mongo:6
    ports:
      - "27017:27017"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  rabbitmq:
    image: rabbitmq:3.12-management
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: password123
    ports:
      - "5672:5672"
      - "15672:15672"
```

```powershell
docker-compose up -d postgres mongodb redis rabbitmq
```

---

## 🐳 Docker

Service sử dụng **multi-stage Docker build** để tối ưu image size:
- **Stage 1 (builder):** `maven:3.9-eclipse-temurin-17` — compile và package
- **Stage 2 (runtime):** `eclipse-temurin:17-jre-alpine` — chỉ chứa JRE, nhẹ và bảo mật

### Build image

```powershell
# Từ root của monorepo
docker build -f chat/Dockerfile -t whatsapp-chat-service:latest .
```

### Chạy container

```powershell
docker run -d `
  --name chat-service `
  -p 8082:8082 `
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/whatsapp `
  -e SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/whatsapp `
  -e SPRING_DATA_REDIS_HOST=redis `
  -e SPRING_RABBITMQ_HOST=rabbitmq `
  whatsapp-chat-service:latest
```

**Bảo mật:** Container chạy dưới non-root user (`appuser`) để tuân thủ security best practices.

---

## 📊 Monitoring & Observability

### Health Checks

```
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
```

Health checks bao gồm: **PostgreSQL**, **MongoDB**, **Redis**, **RabbitMQ**.

### Metrics (Prometheus)

```
GET /actuator/metrics
GET /actuator/prometheus
```

### Logging

- **Console:** pattern `%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n`
- **File:** `logs/chat-service.log` (max 10MB, giữ 30 ngày)
- **Log levels:** `com.whatsapp.chat` → `DEBUG`, `root` → `INFO`

---

## 📏 Business Rules

### Tin nhắn

1. **Sender ≠ Receiver** — Không thể gửi tin nhắn cho chính mình
2. **Nội dung không được rỗng** — Content phải có ít nhất 1 ký tự
3. **Giới hạn độ dài** — Tin nhắn văn bản tối đa **10.000 ký tự**
4. **Trạng thái một chiều** — `SENT → DELIVERED → READ` (không thể đảo ngược)
5. **Auto-delivered khi READ** — Nếu tin nhắn được đọc mà chưa delivered, `deliveredAt` tự động set
6. **Soft-delete** — Xóa tin nhắn không xóa khỏi database, chỉ set `deleted = true`
7. **Giới hạn thời gian xóa** — Chỉ **sender** được xóa và phải trong vòng **1 giờ** sau khi gửi

### Cuộc hội thoại

1. **Uniqueness 1-1** — Mỗi cặp user chỉ có duy nhất một hội thoại 1-1
2. **Participant sort** — ID của 2 người trong hội thoại 1-1 được sort alphabetically để đảm bảo key uniqueness
3. **Quyền truy cập** — Chỉ **thành viên** của hội thoại mới có thể xem/gửi tin nhắn
4. **Group admin** — Người tạo nhóm tự động là **admin**; chỉ admin mới có thể thêm/xóa thành viên
5. **Unread count** — Mỗi thành viên có `unread_count` riêng biệt, reset khi đọc hội thoại

---

## 🔗 Liên quan trong hệ thống

Chat Service là một phần của hệ thống **WhatsApp Clone** gồm nhiều microservices:

```
whatsapp-clone-parent
├── common-lib          # Shared utilities, DTOs, exceptions
├── chat-service        # 👈 Module này
├── user-service        # Quản lý người dùng, profile
├── notification-service # Push notifications
├── media-service       # Upload/lưu trữ media
└── api-gateway         # Entry point, routing, auth
```

Chat Service consume events từ `user-service` (online status) và publish events cho `notification-service` (gửi push notification khi có tin nhắn mới).

---

*© WhatsApp Clone Team — Chat Service v1.0.0-SNAPSHOT*


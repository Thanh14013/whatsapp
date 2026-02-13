# WhatsApp Clone - Microservices Architecture

<div align="center">

![Java](https://img.shields.io/badge/Java-17-red?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen?style=flat-square&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-green?style=flat-square&logo=mongodb)
![Redis](https://img.shields.io/badge/Redis-7.2-red?style=flat-square&logo=redis)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-orange?style=flat-square&logo=rabbitmq)
![Docker](https://img.shields.io/badge/Docker-24.0-blue?style=flat-square&logo=docker)

**A production-ready, scalable real-time chat application built with microservices architecture and Domain-Driven Design (DDD)**

[Features](#-features) • [Architecture](#-architecture) • [Tech Stack](#-tech-stack) • [Getting Started](#-getting-started) • [Documentation](#-documentation)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [System Design](#-system-design)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [API Documentation](#-api-documentation)
- [Performance](#-performance)
- [Monitoring](#-monitoring)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Overview

WhatsApp Clone is a **production-ready, enterprise-grade real-time messaging platform** designed to handle **100 million daily active users (DAU)** with **20 messages per user per day**. The system is built using **microservices architecture** following **Domain-Driven Design (DDD)** principles.

### Key Highlights

- 🚀 **High Performance**: Handles 230K RPS at peak hours
- 📊 **Scalable**: Horizontal scaling with 10+ machines handling 2M concurrent WebSocket connections
- 💾 **Dual Database**: PostgreSQL for metadata + MongoDB for message content
- ⚡ **Real-time**: WebSocket-based instant messaging
- 🔄 **Event-Driven**: RabbitMQ for asynchronous processing
- 📱 **Offline Support**: Push notifications via Firebase Cloud Messaging (FCM)
- 🛡️ **Production Ready**: Docker, health checks, metrics, logging

---

## ✨ Features

### Functional Requirements

#### ✅ Core Messaging
- [x] **Real-time messaging** via WebSocket
- [x] **Message delivery tracking** (sent → delivered → read)
- [x] **Offline message handling** with push notifications
- [x] **Message persistence** (up to 90 days configurable)
- [x] **Duplicate message prevention**
- [x] **Undelivered message storage** (1 year)

#### ✅ User Management
- [x] User registration & authentication (JWT)
- [x] User profile management
- [x] Online/Offline status tracking
- [x] Contact management

#### ✅ Advanced Features
- [x] **Message history** retrieval with pagination
- [x] **Conversation management**
- [x] **Device token management** (multi-device support)
- [x] **Message cleanup policies**
- [x] **Rate limiting** (Bucket4j)

### Non-Functional Requirements

- ⚡ **Performance**: 230K RPS peak capacity
- 🌐 **Availability**: 99.9% uptime with circuit breakers
- 💾 **Storage**: 50TB for 4-month average retention
- 🔒 **Security**: JWT authentication, password hashing (BCrypt)
- 📈 **Scalability**: Horizontal scaling ready
- 🔍 **Observability**: Prometheus metrics, health checks

---

## 🏗️ Architecture

### Microservices Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway                              │
│  • Routing • Authentication • Rate Limiting • Circuit Breaker   │
└────────────────────────┬────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   ┌────▼────┐     ┌────▼────┐     ┌────▼──────┐
   │  User   │     │  Chat   │     │Notification│
   │ Service │     │ Service │     │  Service   │
   └────┬────┘     └────┬────┘     └────┬───────┘
        │               │                │
        │          ┌────▼────────────────▼───┐
        │          │  Message Processor      │
        │          │     Service             │
        │          └─────────────────────────┘
        │                     │
   ┌────▼─────────────────────▼──────────────┐
   │        Infrastructure Layer             │
   │  • PostgreSQL  • MongoDB  • Redis       │
   │  • RabbitMQ    • Firebase (FCM)         │
   └─────────────────────────────────────────┘
```

### Service Responsibilities

| Service | Port | Responsibility | Database |
|---------|------|---------------|----------|
| **API Gateway** | 8080 | Routing, Auth, Rate Limiting | - |
| **User Service** | 8081 | User management, Authentication | PostgreSQL + Redis |
| **Chat Service** | 8082 | Real-time messaging, WebSocket | PostgreSQL + MongoDB + Redis |
| **Message Processor** | 8083 | Offline message handling | MongoDB + Redis |
| **Notification Service** | 8084 | Push notifications (FCM) | Redis |

---

## 🎯 System Design

### Message Flow

#### 1️⃣ **Send Message (Online User)**

```
Client A                Chat Service              RabbitMQ              Client B
   │                         │                       │                     │
   │──① send_message────────>│                       │                     │
   │                         │──② generate_id───────>│                     │
   │                         │──③ write_queue───────>│                     │
   │<─④ message_received─────│                       │                     │
   │                         │                       │──⑤ consume─────────>│
   │                         │                       │                     │
   │                         │<──────────────⑥ incoming_message───────────│
   │                         │──⑦ deliver─────────────────────────────────>│
   │                         │<─────────────⑧ message_delivered───────────│
   │<─⑨ message_delivered────│                       │                     │
```

#### 2️⃣ **Send Message (Offline User)**

```
Client A                Chat Service         Message Processor        Notification Service
   │                         │                       │                          │
   │──① send_message────────>│                       │                          │
   │<─② message_received─────│                       │                          │
   │                         │──③ queue────────────> │                          │
   │                         │                       │──④ detect_offline───────>│
   │                         │                       │                          │──⑤ FCM Push
   │                         │                       │──⑥ cache_inbox(Redis)    │
   │                         │<──────────────⑦ store_db─────────────────────────│
```

#### 3️⃣ **Receive Offline Messages**

```
Client B                Chat Service              Redis Cache           MongoDB
   │                         │                          │                  │
   │──① connect(WebSocket)──>│                          │                  │
   │                         │──② check_inbox──────────>│                  │
   │                         │<─③ undelivered_messages──│                  │
   │<─④ deliver_messages─────│                          │                  │
   │──⑤ message_delivered───>│                          │                  │
   │                         │──⑥ update_status─────────────────────────> │
   │                         │──⑦ clear_inbox──────────>│                  │
```

### Database Strategy

#### **Dual Database Architecture**

**PostgreSQL** (ACID Transactions)
- User profiles & authentication
- Conversation metadata
- Relationships & contacts

**MongoDB** (High Volume, Scalability)
- Message content (millions/day)
- Chat history
- Time-series data

**Redis** (Caching & Speed)
- User online status (5 min TTL)
- Inbox cache (undelivered messages)
- Session management
- User data cache (1 hour TTL)

### Event-Driven Architecture

**RabbitMQ Queues:**

```
user.events (exchange)
├── user.created
├── user.updated
├── user.deleted
└── user.status.changed

message.events (exchange)
├── message.sent
├── message.delivered
└── message.read
```

---

## 🛠️ Tech Stack

### Backend

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Java** | 17 | Primary language |
| **Spring Boot** | 3.2.2 | Application framework |
| **Spring Cloud Gateway** | 2023.0.0 | API Gateway |
| **Spring Data JPA** | 3.2.x | ORM for PostgreSQL |
| **Spring Data MongoDB** | 4.2.x | MongoDB integration |
| **Spring AMQP** | 3.1.x | RabbitMQ messaging |
| **Spring WebSocket** | 6.1.x | Real-time communication |

### Databases & Cache

| Technology | Version | Purpose |
|-----------|---------|---------|
| **PostgreSQL** | 15 | Primary database (metadata) |
| **MongoDB** | 7.0 | Message storage |
| **Redis** | 7.2 | Caching & sessions |

### Infrastructure

| Technology | Version | Purpose |
|-----------|---------|---------|
| **RabbitMQ** | 3.12 | Message queue |
| **Firebase Admin SDK** | 9.2.0 | Push notifications (FCM) |
| **Docker** | 24.0 | Containerization |
| **Docker Compose** | 2.23 | Local orchestration |

### Libraries

- **MapStruct** 1.5.5: Object mapping
- **Lombok**: Boilerplate reduction
- **Flyway**: Database migrations
- **Bucket4j**: Rate limiting
- **Resilience4j**: Circuit breakers
- **Micrometer**: Metrics
- **JWT (jjwt)** 0.12.5: Authentication
- **Snowflake ID**: Distributed ID generation

---

## 📁 Project Structure

```
whatsapp-clone/
├── api-gateway/                 # API Gateway (8080)
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── RateLimitConfig.java
│   │   └── CircuitBreakerConfig.java
│   └── filter/
│       ├── AuthenticationFilter.java
│       └── LoggingFilter.java
│
├── user-service/                # User Service (8081) - DDD
│   ├── domain/                  # Domain Layer
│   │   ├── model/
│   │   │   ├── User.java       # Aggregate Root
│   │   │   ├── UserProfile.java
│   │   │   └── vo/             # Value Objects
│   │   ├── repository/
│   │   └── service/
│   ├── application/             # Application Layer
│   │   ├── service/
│   │   ├── dto/
│   │   └── mapper/
│   ├── infrastructure/          # Infrastructure Layer
│   │   ├── persistence/
│   │   ├── cache/
│   │   ├── messaging/
│   │   └── config/
│   └── interfaces/              # Interface Layer
│       ├── rest/
│       └── exception/
│
├── chat-service/                # Chat Service (8082) - DDD
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Message.java    # Aggregate Root
│   │   │   ├── Conversation.java
│   │   │   └── vo/
│   ├── application/
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── postgres/       # Conversation metadata
│   │   │   └── mongodb/        # Message content
│   │   ├── websocket/
│   │   ├── cache/
│   │   └── messaging/
│   └── interfaces/
│       ├── websocket/
│       └── rest/
│
├── message-processor-service/   # Message Processor (8083)
│   ├── consumer/
│   ├── processor/
│   └── service/
│
├── notification-service/        # Notification Service (8084)
│   ├── service/
│   │   ├── FCMService.java
│   │   ├── DeviceTokenService.java
│   │   └── NotificationService.java
│   ├── consumer/
│   └── config/
│
├── common-lib/                  # Shared Library
│   ├── dto/
│   ├── exception/
│   ├── util/
│   └── constant/
│
├── infrastructure/
│   └── docker/
│       ├── docker-compose.yml
│       └── nginx/
│
└── pom.xml                      # Parent POM
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.9+**
- **Docker & Docker Compose**
- **Firebase Account** (for push notifications)

### Quick Start (Docker)

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/whatsapp-clone.git
cd whatsapp-clone
```

2. **Set up Firebase credentials**
```bash
# Download Firebase service account JSON from Firebase Console
# Project Settings → Service Accounts → Generate New Private Key
export FIREBASE_CREDENTIALS_PATH=/path/to/firebase-credentials.json
```

3. **Start all services**
```bash
cd infrastructure/docker
docker-compose up -d
```

4. **Verify services**
```bash
# API Gateway
curl http://localhost:8080/actuator/health

# User Service
curl http://localhost:8081/actuator/health

# Chat Service
curl http://localhost:8082/actuator/health

# Message Processor
curl http://localhost:8083/actuator/health

# Notification Service
curl http://localhost:8084/actuator/health
```

### Development Setup

1. **Build all services**
```bash
mvn clean install
```

2. **Start infrastructure**
```bash
# PostgreSQL
docker run -d -p 5432:5432 --name postgres \
  -e POSTGRES_DB=whatsapp \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=password123 \
  postgres:15

# MongoDB
docker run -d -p 27017:27017 --name mongodb \
  mongo:7.0

# Redis
docker run -d -p 6379:6379 --name redis \
  redis:7.2-alpine

# RabbitMQ
docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=password123 \
  rabbitmq:3.12-management
```

3. **Run services individually**
```bash
# API Gateway
mvn spring-boot:run -pl api-gateway

# User Service
mvn spring-boot:run -pl user-service

# Chat Service
mvn spring-boot:run -pl chat-service

# Message Processor
mvn spring-boot:run -pl message-processor-service

# Notification Service
mvn spring-boot:run -pl notification-service
```

---

## 📚 API Documentation

### User Service API

#### Register User
```http
POST /users
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "phoneNumber": "+1234567890",
  "password": "SecurePass123!",
  "displayName": "John Doe"
}
```

#### Get Current User
```http
GET /users/me
Authorization: Bearer <jwt-token>
```

### Chat Service API (WebSocket)

#### Connect
```javascript
const ws = new WebSocket('ws://localhost:8082/ws/chat?userId=user-123');
```

#### Send Message
```json
{
  "action": "send_message",
  "receiverId": "user-456",
  "content": "Hello!",
  "contentType": "TEXT"
}
```

#### Message Received (Server → Client)
```json
{
  "type": "incoming_message",
  "message": {
    "id": "msg-789",
    "senderId": "user-123",
    "content": "Hello!",
    "createdAt": "2026-02-13T10:00:00Z"
  }
}
```

### Notification Service API

#### Register Device Token
```http
POST /notifications/register
Content-Type: application/json

{
  "userId": "user-123",
  "token": "fcm-device-token-xxx",
  "platform": "ANDROID"
}
```

For complete API documentation, visit: [API Docs](./docs/api/)

---

## ⚡ Performance

### Resource Estimation

**Users & Traffic:**
- **100M DAU** (Daily Active Users)
- **20 messages/user/day** average
- **~23K RPS** average
- **230K RPS** peak (10x average)

**Storage:**
- **50TB** for 4-month average retention
- **200 bytes** per message (with metadata)
- **100M × 20 × 200 × 31 × 4** = ~50TB

**Infrastructure:**
- **10 machines** for WebSocket (1M connections each)
- **2M concurrent** WebSocket connections
- **HikariCP** connection pooling (max 10 per service)

### Optimization Strategies

1. **Caching**
    - Redis for user status (5 min TTL)
    - Inbox cache for offline messages
    - User data cache (1 hour TTL)

2. **Database Sharding**
    - Partition by `user_id` for even distribution
    - MongoDB sharding for message scaling

3. **Async Processing**
    - RabbitMQ for decoupling
    - Event-driven architecture
    - Non-blocking I/O

4. **Connection Management**
    - WebSocket connection pooling
    - Load balancing across chat servers
    - Sticky sessions for WebSocket

---

## 📊 Monitoring

### Health Checks

All services expose health endpoints:
```bash
curl http://localhost:8080/actuator/health
```

### Metrics (Prometheus)

```bash
# API Gateway
curl http://localhost:8080/actuator/prometheus

# User Service
curl http://localhost:8081/actuator/prometheus

# Chat Service
curl http://localhost:8082/actuator/prometheus
```

### Key Metrics

- **Request Rate**: `http_server_requests_seconds_count`
- **Response Time**: `http_server_requests_seconds_sum`
- **WebSocket Connections**: `websocket_connections_active`
- **Message Queue Size**: `rabbitmq_queue_messages_ready`
- **Cache Hit Rate**: `cache_gets_total`
- **Database Connections**: `hikaricp_connections_active`

### Logging

All services use structured logging:
```
2026-02-13 10:00:00 [main] INFO  c.w.chat.ChatService - Message sent: msg-123
```

Logs are stored in:
- `logs/api-gateway.log`
- `logs/user-service.log`
- `logs/chat-service.log`
- `logs/message-processor.log`
- `logs/notification-service.log`

---

## 🔒 Security

### Authentication
- **JWT tokens** (24-hour expiration)
- **Refresh tokens** (7-day expiration)
- **BCrypt** password hashing (strength 12)

### Authorization
- **Role-based** access control
- **User-level** permissions
- **API Gateway** authentication filter

### Rate Limiting
- **Bucket4j** implementation
- **100 requests/minute** per user
- **Configurable** per endpoint

### Data Protection
- **TLS/SSL** for all communications
- **Password policies** (8+ chars, uppercase, lowercase, digit, special)
- **Account lockout** (5 failed attempts, 15 min)

---

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Load Testing
```bash
# Using Apache Bench
ab -n 10000 -c 100 http://localhost:8080/users/me
```

---

## 📈 Scalability

### Horizontal Scaling

**Stateless Services** (Easy to scale):
- API Gateway
- User Service
- Message Processor
- Notification Service

**Stateful Service** (Requires coordination):
- Chat Service (WebSocket connections)
    - Use Redis for session storage
    - Sticky sessions in load balancer
    - Connection registry for routing

### Database Scaling

**PostgreSQL:**
- Read replicas for queries
- Sharding by `user_id`
- Connection pooling (HikariCP)

**MongoDB:**
- Replica sets (1 primary + 2 secondaries)
- Sharding by `user_id`
- Index optimization

**Redis:**
- Master-slave replication
- Redis Cluster for distribution
- Separate instances per service

---

## 🐳 Docker Deployment

### Build Images
```bash
# Build all services
docker build -t whatsapp-clone/api-gateway:latest -f api-gateway/Dockerfile .
docker build -t whatsapp-clone/user-service:latest -f user-service/Dockerfile .
docker build -t whatsapp-clone/chat-service:latest -f chat-service/Dockerfile .
docker build -t whatsapp-clone/message-processor:latest -f message-processor-service/Dockerfile .
docker build -t whatsapp-clone/notification-service:latest -f notification-service/Dockerfile .
```

### Run with Docker Compose
```bash
cd infrastructure/docker
docker-compose up -d
```

### Environment Variables

Create `.env` file:
```env
# Database
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=whatsapp
POSTGRES_USER=admin
POSTGRES_PASSWORD=password123

# MongoDB
MONGO_HOST=mongodb
MONGO_PORT=27017

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=admin
RABBITMQ_PASSWORD=password123

# Firebase
FIREBASE_CREDENTIALS_PATH=/app/config/firebase-credentials.json

# JWT
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400000
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Coding Standards
- Follow **DDD** principles
- Write **unit tests** for all business logic
- Document **public APIs** with JavaDoc
- Use **meaningful** commit messages
- Follow **Spring Boot** best practices

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **System Design**: Based on [System Design School](https://systemdesignschool.io)
- **Spring Boot**: [Spring Framework](https://spring.io)
- **Domain-Driven Design**: Eric Evans
- **Microservices Patterns**: Chris Richardson

---

## 📞 Contact

**Project Maintainer**: Your Name

- Email: your.email@example.com
- GitHub: [@yourusername](https://github.com/yourusername)
- LinkedIn: [Your Profile](https://linkedin.com/in/yourprofile)

---

## 🗺️ Roadmap

### Phase 1 (Current) ✅
- [x] Basic messaging infrastructure
- [x] User management
- [x] Real-time WebSocket communication
- [x] Offline message handling
- [x] Push notifications

### Phase 2 (Planned)
- [ ] Group chat support
- [ ] Media messages (images, videos)
- [ ] Voice/Video calls
- [ ] End-to-end encryption
- [ ] Message reactions & replies

### Phase 3 (Future)
- [ ] Stories feature
- [ ] Status updates
- [ ] Payment integration
- [ ] Bot API
- [ ] Analytics dashboard

---

<div align="center">

**⭐ Star this repository if you find it helpful!**

Made with ❤️ by [Your Name](https://github.com/yourusername)

</div>
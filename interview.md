# Interview Q&A — WhatsApp Clone Project

> Tổng hợp các câu hỏi phỏng vấn thường gặp và câu trả lời chuẩn xác về dự án.
> Tất cả bằng tiếng Việt.

---

## Mục lục

1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Kiến trúc Microservices](#2-kiến-trúc-microservices)
3. [Domain-Driven Design (DDD)](#3-domain-driven-design-ddd)
4. [Real-time & WebSocket](#4-real-time--websocket)
5. [Database Strategy](#5-database-strategy)
6. [Redis & Caching](#6-redis--caching)
7. [Event-Driven & RabbitMQ](#7-event-driven--rabbitmq)
8. [Security](#8-security)
9. [Rate Limiting & Resilience](#9-rate-limiting--resilience)
10. [Push Notification](#10-push-notification)
11. [Scheduled Jobs & Cleanup](#11-scheduled-jobs--cleanup)
12. [Monitoring & Observability](#12-monitoring--observability)
13. [Docker & DevOps](#13-docker--devops)
14. [Scalability](#14-scalability)
15. [Testing](#15-testing)
16. [Design Patterns](#16-design-patterns)

---

## 1. Tổng quan dự án

---

**Q: Bạn có thể mô tả tổng quan dự án này trong 2–3 câu không?**

> Đây là một ứng dụng nhắn tin thời gian thực theo mô hình clone WhatsApp, được xây dựng theo kiến trúc microservices với Domain-Driven Design. Hệ thống gồm 6 service độc lập giao tiếp qua REST API và RabbitMQ, hỗ trợ nhắn tin real-time qua WebSocket, thông báo đẩy (FCM/APNs) cho người dùng offline, và theo dõi trạng thái tin nhắn (sent → delivered → read). Toàn bộ infrastructure được container hoá với Docker và có observability đầy đủ qua Prometheus, Grafana và ELK stack.

---

**Q: Tại sao bạn chọn xây dựng dự án này?**

> WhatsApp là một ví dụ điển hình về hệ thống phức tạp ở production scale, đòi hỏi kết hợp nhiều kỹ thuật nâng cao: real-time communication, event-driven architecture, polyglot persistence, và distributed system design. Tôi muốn có một dự án thực tế để áp dụng toàn bộ những kiến thức về microservices, DDD, caching strategy, message queue và observability trong cùng một codebase nhất quán, thay vì học từng mảnh rời rạc.

---

**Q: Dự án này có những tính năng chính nào?**

> - **Nhắn tin thời gian thực** qua WebSocket với STOMP protocol
> - **Delivery status tracking**: trạng thái tin nhắn tự động chuyển SENT → DELIVERED → READ
> - **Offline message handling**: tin nhắn được cache vào Redis inbox, giao lại khi user reconnect
> - **Push Notification**: FCM cho Android/Web, APNs cho iOS khi người nhận offline
> - **Multi-device support**: 1 user có thể đăng nhập từ nhiều thiết bị, mỗi thiết bị nhận tin nhắn
> - **User authentication**: JWT + BCrypt, đăng ký/đăng nhập, quản lý profile
> - **Presence status**: online/offline/away/busy, hiển thị realtime
> - **Lịch sử tin nhắn**: phân trang, retention policy 90 ngày, tự động cleanup

---

**Q: Dự án này có bao nhiêu service? Mỗi service chịu trách nhiệm gì?**

> Có **6 service** chính:
> | Service | Port | Trách nhiệm |
> |---|---|---|
> | API Gateway | 8080 | Routing, JWT auth, Rate limiting, Circuit breaker |
> | User Service | 8081 | Đăng ký/đăng nhập, quản lý profile, presence status |
> | Chat Service | 8082 | WebSocket, gửi/nhận tin nhắn, quản lý conversation |
> | Message Processor | 8083 | Xử lý tin nhắn offline, caching inbox, trigger notification |
> | Notification Service | 8084 | Gửi push notification qua FCM/APNs |
> | Scheduled Jobs | 8086 | Cleanup tin nhắn cũ, cache cleanup, user policy |

---

## 2. Kiến trúc Microservices

---

**Q: Tại sao bạn chọn microservices thay vì monolithic?**

> Với một hệ thống như WhatsApp, các tài nguyên cần scale khác nhau: Chat Service cần scale nhiều vì xử lý WebSocket connections, trong khi Notification Service chỉ cần scale khi có nhiều user offline. Microservices cho phép scale từng service độc lập. Ngoài ra, các team có thể làm việc độc lập trên từng service mà không conflict. Tuy nhiên tôi cũng nhận thức được trade-off: microservices phức tạp hơn về network latency, distributed transactions, và deployment.

---

**Q: Các service giao tiếp với nhau bằng cách nào?**

> Có 2 hình thức:
> - **Synchronous (REST)**: API Gateway gọi các service qua HTTP. Ví dụ: Gateway forward request đến User Service hay Chat Service.
> - **Asynchronous (RabbitMQ)**: Các service publish/consume events qua message queue. Ví dụ: Chat Service publish `message.sent` event → Message Processor consume → nếu receiver offline thì publish tiếp `offline.notification` → Notification Service consume và gửi FCM. Cách này giúp loose coupling: Chat Service không cần biết Notification Service tồn tại.

---

**Q: common-lib là gì? Tại sao lại có module này?**

> `common-lib` là một shared library chứa các thứ dùng chung giữa tất cả services: DTOs, Exception classes (`BusinessException`, `ResourceNotFoundException`), utility methods, constants, và `PageResponse<T>` wrapper để chuẩn hoá pagination response. Nếu không có `common-lib`, mỗi service phải tự định nghĩa lại các class này, dẫn đến code duplicate và inconsistency. Maven multi-module với parent POM quản lý version tập trung giúp tất cả service dùng cùng version thư viện.

---

**Q: Làm thế nào để đảm bảo các service không phụ thuộc "vòng tròn" lẫn nhau?**

> Thiết kế luồng luôn theo một chiều:
> Gateway → User/Chat/Notification Service (synchronous)
> Chat Service → Message Processor → Notification Service (asynchronous qua RabbitMQ)
> Không có service nào gọi ngược lại service đã gọi mình. Các event-driven flow qua RabbitMQ giúp tách biệt hoàn toàn: producer không biết consumer, tránh circular dependency.

---

**Q: API Gateway trong dự án này làm gì cụ thể?**

> API Gateway được build trên **Spring Cloud Gateway** (reactive/Netty), xử lý theo pipeline:
> 1. `LoggingFilter`: gắn request ID, log timing
> 2. `AuthenticationFilter`: validate JWT, inject `X-User-Id`, `X-Username`, `X-User-Roles` headers vào request để downstream service không cần parse JWT lại
> 3. `RateLimiterFilter`: Bucket4j token bucket, chặn request khi vượt quota
> 4. `CircuitBreakerFilter`: Resilience4j, mở circuit sau 50% failure trong 10s window
> 5. `RetryFilter`: 3 lần retry với exponential backoff
> 6. Route đến service tương ứng theo path

---

## 3. Domain-Driven Design (DDD)

---

**Q: DDD là gì và bạn áp dụng nó như thế nào trong dự án?**

> DDD là cách thiết kế phần mềm tập trung vào business domain. Tôi áp dụng theo **Hexagonal Architecture** với 4 layer:
> - **Domain Layer**: chứa Aggregate Root (`User`, `Message`, `Conversation`), Value Objects (`Email`, `PhoneNumber`, `MessageId`), Domain Services, Repository interfaces. Không phụ thuộc bất kỳ framework nào.
> - **Application Layer**: chứa Use Case services (`UserApplicationService`, `ChatApplicationService`), DTOs, Mappers. Orchestrate domain objects.
> - **Infrastructure Layer**: implement Repository interfaces bằng JPA/MongoDB, Redis cache, RabbitMQ publisher.
> - **Interface Layer**: REST controllers, WebSocket controllers — chỉ là adapter từ HTTP/WS ra application layer.

---

**Q: Aggregate Root là gì? Trong project này có những Aggregate Root nào?**

> Aggregate Root là entry point duy nhất để truy cập và modify một nhóm đối tượng domain liên quan. Mọi thay đổi bên trong aggregate phải đi qua root để đảm bảo business invariants luôn được thoả mãn.
> Trong project có 3 Aggregate Root:
> - `User`: kiểm soát `UserProfile`, `UserStatus`. Không ai được phép modify profile trực tiếp mà phải gọi `user.updateProfile()`.
> - `Message`: kiểm soát lifecycle — trạng thái chỉ được chuyển từ SENT → DELIVERED → READ theo thứ tự, logic nằm trong `message.markAsDelivered()`, `message.markAsRead()`.
> - `Conversation`: kiểm soát danh sách participants, unread counts, lastMessageId. Chỉ admin mới thêm/xoá participant trong group.

---

**Q: Value Object là gì? Tại sao dùng Value Object thay vì plain String?**

> Value Object là đối tượng immutable, tự validate, không có identity riêng. Ví dụ: `Email.of("test@example.com")` — constructor validate format email, nếu sai throw exception ngay lập tức. So với dùng `String email`, Value Object đảm bảo:
> 1. Không bao giờ có Email invalid trong hệ thống
> 2. Không nhầm lẫn giữa email và phoneNumber (type safety)
> 3. Business logic (validation) nằm trong domain, không bị scatter ra ngoài
> Các Value Object trong project: `Email`, `PhoneNumber`, `UserId`, `MessageId`, `ConversationId`, `MessageContent`.

---

**Q: Sự khác biệt giữa Domain Service và Application Service là gì?**

> - **Domain Service** (`UserDomainService`): chứa business logic liên quan đến nhiều entity, không fit vào một entity cụ thể. Ví dụ: `validateUserUniqueness()` — cần query repository để kiểm tra username/email/phone đã tồn tại chưa, logic này không thể để trong `User` entity vì User không tự query được.
> - **Application Service** (`UserApplicationService`): orchestrate use case, không chứa business logic. Gọi domain service, repository, cache, event publisher theo đúng thứ tự. Ví dụ `createUser()`: validate uniqueness → encode password → create User → save → publish event.

---

**Q: Tại sao dùng static factory method `User.create()` thay vì `new User()`?**

> Static factory method enforce business rules ngay tại thời điểm tạo object:
> ```java
> public static User create(username, email, phone, passwordHash, profile) {
>     validateUsername(username);  // business rule check
>     // ...
>     return new User(..., UserStatus.OFFLINE, true, false, false, now, now);
> }
> ```
> Constructor private, bên ngoài không thể tạo `User` ở trạng thái không hợp lệ. Nếu dùng `new User()` public, caller có thể truyền sai tham số, bỏ qua validation. Factory method cũng đặt default values logic tập trung: user mới luôn OFFLINE, active=true, không verify email/phone.

---

## 4. Real-time & WebSocket

---

**Q: WebSocket trong dự án hoạt động như thế nào?**

> Client kết nối đến Chat Service qua `ws://host:8082/ws/chat` với JWT token trong query string. Server validate JWT, gắn userId vào STOMP session. WebSocket Session Manager (`WebSocketSessionManager`) track mapping `userId → Set<sessionId>` bằng `ConcurrentHashMap`.
>
> Khi gửi tin nhắn: client gửi STOMP frame đến `/app/chat.message` → `ChatWebSocketController.sendMessage()` → `ChatApplicationService.sendMessage()` → persist vào MongoDB → publish `message.sent` event lên RabbitMQ → nếu receiver đang online thì `sessionManager.sendToUser(receiverId, "/queue/messages", response)` ngay lập tức.

---

**Q: STOMP protocol là gì? Tại sao dùng STOMP thay vì raw WebSocket?**

> STOMP (Simple Text Oriented Messaging Protocol) là một protocol message trên WebSocket cung cấp:
> - **Destinations**: `/app/chat.message`, `/user/{id}/queue/messages`, `/topic/conversation.{id}` — phân loại route rõ ràng
> - **Subscriptions**: client subscribe vào destination, server push khi có dữ liệu
> - **Message headers**: metadata đi kèm mỗi frame
>
> Nếu dùng raw WebSocket, phải tự implement protocol on top, tự parse message type, tự quản lý subscriptions. STOMP + Spring MessageBroker làm điều đó sẵn, giảm boilerplate code đáng kể.

---

**Q: Làm sao detect user là online hay offline?**

> Khi user **kết nối WebSocket**: Chat Service update Redis key `user:status:{userId}` = "ONLINE" với TTL 5 phút và thêm session vào `WebSocketSessionManager`.
> Khi user **ngắt kết nối**: xoá Redis key, remove session.
> Khi cần **check status**: Message Processor gọi `UserStatusService.isUserOnline(receiverId)` → query Redis. Nếu không có key hoặc key đã expire → offline.
> TTL 5 phút là fail-safe: nếu service crash mà không kịp set OFFLINE, Redis tự expire sau 5 phút.

---

**Q: Khi user A gửi message cho user B đang offline, flow xử lý như thế nào?**

> 1. Chat Service nhận message từ user A, persist vào MongoDB, cập nhật conversation metadata ở PostgreSQL
> 2. Chat Service publish event `message.sent` vào RabbitMQ exchange `message.events`
> 3. Message Processor consume event → gọi `isUserOnline(B)` → Redis không có key → offline
> 4. Message Processor gọi `InboxCacheService.addToInbox(B, messageId)` → lưu messageId vào Redis Set `inbox:{B}` với TTL 1 năm
> 5. Message Processor gọi `PushNotificationService.sendMessageNotification(B, ...)` → publish event đến Notification Service
> 6. Notification Service lấy device tokens của B từ Redis (`device_tokens:{B}`) → gửi FCM/APNs
> 7. Khi B reconnect WebSocket → Chat Service check Redis `inbox:{B}` → fetch messages từ MongoDB → deliver qua WebSocket → clear inbox

---

**Q: Nếu scale Chat Service lên nhiều instance, WebSocket routing xử lý thế nào?**

> Đây là vấn đề stateful scaling. WebSocket connection là stateful — user A kết nối đến instance 1, user B kết nối đến instance 2. Khi A gửi message cho B, instance 1 không thể dùng `sessionManager` của instance 2.
>
> Giải pháp trong project:
> 1. **Sticky sessions** tại Nginx: consistent hash theo `userId` → cùng user luôn route đến cùng instance
> 2. **Redis Pub/Sub**: instance nhận message publish vào Redis channel → tất cả instance subscribe và forward đến user kết nối với mình
> 3. **Connection Registry**: Redis lưu `user:instance:{userId}` → biết user đang kết nối instance nào

---

## 5. Database Strategy

---

**Q: Tại sao dùng 2 database (PostgreSQL và MongoDB) thay vì chỉ 1?**

> Đây là **Polyglot Persistence** — dùng đúng loại DB cho đúng bài toán:
> - **PostgreSQL** (relational, ACID): lưu user profiles, conversation metadata, participants — dữ liệu cần integrity cao, join queries, foreign key constraints.
> - **MongoDB** (document, scale-out): lưu message content — hàng tỷ tin nhắn, schema flexible (text/image/video có cấu trúc khác nhau), cần scale storage cao, TTL index tự động xoá.
>
> Nếu dùng chỉ PostgreSQL: message volume quá lớn, sẽ gây chậm cả user queries. Nếu chỉ MongoDB: thiếu ACID transactions cho user/auth data.

---

**Q: Schema thiết kế trong MongoDB như thế nào?**

> Message document trong MongoDB:
> ```json
> {
>   "_id": "01HN...",          // Snowflake ID
>   "conversationId": "...",
>   "senderId": "...",
>   "receiverId": "...",
>   "content": "Hello!",
>   "contentType": "TEXT",
>   "status": "READ",
>   "deleted": false,
>   "sentAt": ISODate("..."),
>   "deliveredAt": ISODate("..."),
>   "readAt": ISODate("...")
> }
> ```
> **Indexes**:
> - `{ conversationId: 1, createdAt: -1 }` — pagination lịch sử chat
> - `{ senderId: 1, createdAt: -1 }` — lịch sử gửi
> - TTL index: `expireAfterSeconds: 7776000` (90 ngày) — tự động xoá

---

**Q: Tại sao dùng Snowflake ID thay vì UUID hoặc MongoDB ObjectId?**

> - **UUID**: random, không sortable theo thời gian → index kém hiệu quả
> - **MongoDB ObjectId**: 12 bytes, sortable nhưng gắn với MongoDB, không dùng được ở PostgreSQL
> - **Snowflake ID**: 64-bit integer, **time-sortable** (41 bit timestamp đầu), globally unique, không cần coordination giữa nodes, có thể dùng ở cả MongoDB lẫn PostgreSQL. Với pattern `{ conversationId, createdAt }` trong index, Snowflake ID còn cho phép extract timestamp từ ID nên không cần JOIN để lấy thời gian.

---

**Q: Flyway là gì? Tại sao cần database migration?**

> Flyway là công cụ version control cho database schema. Mỗi thay đổi schema được viết thành file SQL có tên `V1__create_users.sql`, `V2__add_conversations.sql`... Flyway track version đã apply trong bảng `flyway_schema_history`.
>
> Tại sao cần:
> - **Reproducible**: deploy lên môi trường mới, schema tự động được tạo đúng version
> - **Team collaboration**: ai cũng biết schema đang ở version nào
> - **Rollback**: có thể viết migration ngược
> - Không dùng Flyway → `spring.jpa.hibernate.ddl-auto=create-drop` rất nguy hiểm ở production (xoá hết data khi restart)

---

## 6. Redis & Caching

---

**Q: Redis được dùng để làm gì trong dự án này?**

> Redis đóng nhiều vai trò:
> | Key pattern | Dùng cho | TTL |
> |---|---|---|
> | `user:{id}` (Hash) | Cache user profile | 1 giờ |
> | `user:status:{id}` (String) | Presence status (ONLINE/OFFLINE) | 5 phút |
> | `inbox:{userId}` (Set) | Message IDs chưa giao | 1 năm |
> | `device_tokens:{userId}` (Set) | Push notification tokens | 30 ngày |
> | `session:{token}` (String) | JWT session, dùng để revoke | 24 giờ |
> | `rate_limit:{ip}:{endpoint}` (String) | Token bucket counter | 60 giây |

---

**Q: Write-through caching là gì? Bạn implement thế nào?**

> Write-through: khi ghi vào DB thì đồng thời ghi luôn vào cache. Ví dụ trong `UserApplicationService.updateUser()`:
> ```java
> User savedUser = userRepository.save(user);  // ghi DB
> cacheService.cacheUser(userMapper.toDto(savedUser));  // ghi cache
> ```
> Lần đọc tiếp theo hit cache, không cần query DB. Ngược lại là **write-behind** (ghi cache trước, DB sau) — không dùng vì risk mất data nếu Redis restart.

---

**Q: Cache invalidation trong project được xử lý thế nào?**

> - **Profile update**: `UserApplicationService.updateUser()` → sau khi save DB, gọi `cacheService.evictUser(userId)` → lần đọc sau sẽ miss cache → load lại từ DB và cache mới
> - **Status TTL**: `user:status:{id}` có TTL 5 phút, tự expire — không cần manual invalidate
> - **Device token removal**: khi FCM trả về `UNREGISTERED` → gọi `deviceTokenService.removeToken(token)` → xoá khỏi Redis Set

---

**Q: Tại sao dùng Redis Set cho inbox thay vì Redis List?**

> - **Set**: không có duplicate (cùng messageId không bị add 2 lần), `SISMEMBER` check O(1), `SMEMBERS` lấy tất cả IDs
> - **List**: cho phép duplicate, phù hợp khi order quan trọng
>
> Inbox chỉ cần lưu messageIDs để "biết cần deliver cái gì" — không cần order (order lấy từ MongoDB sau), và cần đảm bảo không duplicate. Redis Set là lựa chọn tự nhiên.

---

## 7. Event-Driven & RabbitMQ

---

**Q: Tại sao dùng RabbitMQ thay vì gọi API trực tiếp giữa các service?**

> Gọi trực tiếp (synchronous) có vấn đề:
> - Nếu Notification Service down → Chat Service cũng fail → user không gửi được tin
> - Chat Service phải đợi Notification Service respond → tăng latency
> - Coupling chặt: Chat biết địa chỉ Notification
>
> RabbitMQ (asynchronous) giải quyết:
> - **Decoupling**: Chat chỉ cần publish event, không biết ai consume
> - **Resilience**: Nếu Notification Service down, message nằm trong queue, khi lên lại thì tự consume
> - **Backpressure**: Consumer tự điều chỉnh tốc độ xử lý
> - **Durable queue**: message không mất khi service restart

---

**Q: Exchange và Queue trong RabbitMQ project này được thiết kế thế nào?**

> Project dùng **Direct Exchange**:
> ```
> user.events (exchange)
>   ├── user.created (queue)
>   ├── user.updated (queue)
>   └── user.status.changed (queue)
>
> message.events (exchange)
>   ├── message.sent (queue)      ← Message Processor consume
>   ├── message.delivered (queue) ← Chat Service consume (update status)
>   └── message.read (queue)      ← Chat Service consume (update status)
>
> notification.events (exchange)
>   └── offline.notification (queue) ← Notification Service consume
> ```
> Mỗi service declare queue riêng và bind vào exchange với routing key tương ứng.

---

**Q: Dead Letter Queue là gì? Tại sao cần?**

> DLQ (Dead Letter Queue) là queue chứa những message xử lý thất bại sau số lần retry tối đa. Ví dụ: Message Processor nhận `message.sent` event nhưng crash liên tục sau 3 lần retry → thay vì bỏ luôn message, RabbitMQ route vào DLQ `message.sent.dlq`. Từ DLQ, team có thể:
> - Inspect message để debug
> - Replay message sau khi fix lỗi
> - Alert monitoring khi DLQ có message
>
> Không có DLQ → message bị lost hoàn toàn → silent failure.

---

**Q: Retry logic cho RabbitMQ được implement thế nào?**

> Dùng Spring AMQP retry configuration:
> - Max attempts: 3
> - Initial interval: 1 giây
> - Multiplier: 2.0 (exponential backoff)
> - Max interval: 10 giây
>
> Tức là: lần 1 fail → đợi 1s → lần 2 fail → đợi 2s → lần 3 fail → đợi 4s → fail hết → route vào DLQ. Exponential backoff tránh hammering downstream service khi nó đang bị quá tải.

---

## 8. Security

---

**Q: JWT authentication trong project hoạt động như thế nào?**

> Flow:
> 1. User POST `/api/auth/login` → User Service validate credentials → generate JWT (HS256, 24h expiry) với claims: `sub=userId`, `username`, `email`, `roles`
> 2. Client gắn token vào header: `Authorization: Bearer <JWT>`
> 3. API Gateway có `AuthenticationFilter implements WebFilter` intercept mọi request
> 4. Filter extract token → validate signature bằng secret key → extract claims
> 5. Inject `X-User-Id`, `X-Username`, `X-User-Roles` headers vào request
> 6. Forward đến downstream service — service chỉ cần đọc header, không cần parse JWT lại

---

**Q: Tại sao dùng stateless JWT thay vì session-based auth?**

> Session-based: server lưu session trong memory/DB → cần sticky session khi scale → stateful.
> JWT stateless: token self-contained, chứa đủ thông tin, server không cần lưu gì → scale horizontally dễ dàng, bất kỳ instance nào cũng validate được token bằng cùng secret key.
>
> Trade-off của JWT: không thể invalidate ngay lập tức nếu bị compromised (phải đợi expire). Project giải quyết bằng cách lưu session vào Redis `session:{token}` với TTL 24h → khi logout, xoá Redis key → token bị revoke dù chưa expire.

---

**Q: BCrypt password hashing hoạt động thế nào? Tại sao strength 12?**

> BCrypt là adaptive hashing function có salt tích hợp sẵn. Mỗi lần hash ra kết quả khác nhau dù cùng input, tránh rainbow table attack. `strength 12` nghĩa là 2^12 = 4096 iterations — đủ chậm để brute force khó, đủ nhanh để user không thấy lag khi login (khoảng 200-300ms trên modern hardware).
>
> Không bao giờ lưu plain-text password. Spring Security `BCryptPasswordEncoder` tự handle salt và so sánh hash khi `passwordEncoder.matches(rawPassword, storedHash)`.

---

**Q: Account lockout được implement như thế nào?**

> Sau **5 lần đăng nhập sai liên tiếp**, tài khoản bị lock 15 phút. Implementation dùng Redis counter `login_attempts:{userId}` với TTL 15 phút:
> - Mỗi lần fail: `INCR login_attempts:{userId}`
> - Nếu counter >= 5: từ chối login, trả về 423 Locked
> - Khi TTL expire (15 phút): counter tự xoá, user thử lại được
> - Login thành công: reset counter về 0

---

**Q: CORS và CSRF được xử lý thế nào?**

> - **CORS**: cấu hình global trong Gateway cho phép các origin cụ thể, các HTTP methods, và custom headers. Toàn bộ traffic vào từ Gateway nên chỉ cần config 1 chỗ.
> - **CSRF**: **disabled** — hợp lý vì đây là stateless REST API dùng JWT Bearer token, không dùng cookie-based session. CSRF attack chỉ relevant khi server dùng session cookie.

---

## 9. Rate Limiting & Resilience

---

**Q: Rate limiting được implement bằng gì? Hoạt động thế nào?**

> Dùng **Bucket4j** với **Token Bucket Algorithm**:
> - Anonymous: 10 req/phút
> - Authenticated: 100 req/phút
> - Premium: 500 req/phút
> - WebSocket messages: 50 msg/phút/connection
>
> Mỗi user/IP có 1 "bucket" chứa tokens. Mỗi request tiêu thụ 1 token. Tokens được refill theo rate cố định. Nếu bucket rỗng → HTTP 429 Too Many Requests.
>
> Ngoài ra, Nginx layer cũng có `limit_req_zone` — 100 req/s cho API, 10 req/s cho auth endpoints — như lớp protection đầu tiên trước khi vào Gateway.

---

**Q: Circuit Breaker là gì? Tại sao cần?**

> Circuit Breaker là pattern bảo vệ hệ thống khỏi cascade failure. Nếu User Service đang chậm/down, Gateway tiếp tục forward request sẽ gây thread pool exhaustion và ảnh hưởng toàn bộ hệ thống.
>
> 3 trạng thái:
> - **CLOSED**: bình thường, request pass through
> - **OPEN**: khi failure rate > 50% trong 10s window → tất cả request fail fast luôn, không gọi downstream
> - **HALF_OPEN**: sau timeout (60s), cho một số request thử → nếu pass, về CLOSED; nếu fail, về OPEN
>
> Dùng **Resilience4j** vì library nhỏ, annotation-based, tích hợp tốt với Spring.

---

**Q: Sự khác biệt giữa Rate Limiting và Circuit Breaker là gì?**

> - **Rate Limiting**: bảo vệ **downstream service** khỏi bị quá tải — kiểm soát số request **từ client vào**
> - **Circuit Breaker**: bảo vệ **hệ thống hiện tại** khỏi chờ đợi vô ích một service đang hỏng — kiểm soát request **đi ra downstream**
>
> Hai cơ chế này bổ sung cho nhau, không thay thế nhau.

---

## 10. Push Notification

---

**Q: Push notification flow hoạt động thế nào khi user offline?**

> 1. Message Processor phát hiện receiver offline qua Redis status check
> 2. Publish event `offline.notification` vào RabbitMQ → Notification Service consume
> 3. Notification Service gọi `deviceTokenService.getTokensForUser(userId)` → lấy tất cả device tokens từ Redis Set `device_tokens:{userId}`
> 4. Với mỗi token: gọi FCM (Android/Web) hoặc APNs (iOS) API
> 5. Nếu FCM trả về `UNREGISTERED` → token không còn valid → tự động remove khỏi Redis
> 6. Ghi metrics: `notification_sent_total`, `notification_failed_total`

---

**Q: Multi-device support được implement thế nào?**

> Mỗi device khi login đăng ký device token với Notification Service → token được add vào Redis Set `device_tokens:{userId}`. Dùng Set để tránh duplicate (cùng device login nhiều lần chỉ 1 token).
> Khi gửi notification, lấy toàn bộ tokens của user (SMEMBERS) → gửi đến tất cả → user nhận notify trên tất cả thiết bị đang active.

---

**Q: Tại sao gửi notification bất đồng bộ (`@Async`)?**

> Push notification gọi FCM/APNs API là I/O-bound, có thể mất 100ms–1s. Nếu synchronous, Message Processor thread bị block trong thời gian này, giảm throughput. Với `@Async`, notification được xử lý trên thread pool riêng, không block main processing flow. Spring quản lý thread pool thông qua `@EnableAsync` và `TaskExecutor` bean.

---

## 11. Scheduled Jobs & Cleanup

---

**Q: Scheduled Jobs service làm gì? Tại sao tách ra service riêng?**

> Scheduled Jobs có 3 job chính:
> - `MessageCleanupJob`: xoá tin nhắn chưa giao > 1 năm (2AM), xoá tin nhắn đã giao > 90 ngày (3AM), xoá soft-deleted messages sau grace period (4AM)
> - `CacheCleanupJob`: scan Redis inbox, clear orphan entries mỗi 6 tiếng
> - `UserPolicyCleanupJob`: xoá accounts inactive > 365 ngày sau last login (3AM)
>
> Tách service riêng vì: job này resource-intensive (scan MongoDB/PostgreSQL), nên chạy isolated không ảnh hưởng đến Chat Service hay User Service đang phục vụ user. Cũng dễ scale down khi không cần (1 instance là đủ).

---

**Q: Soft delete là gì? Tại sao không hard delete ngay?**

> Soft delete: set `deleted = true` và `deletedAt = now()`, không xoá row/document thật sự. Hard delete thực hiện sau một grace period (30 ngày).
>
> Lý do:
> - **Accident recovery**: admin có thể khôi phục nếu xoá nhầm
> - **Audit trail**: biết khi nào và ai xoá
> - **Sync consistency**: các service khác có thể đã cache reference đến message này, hard delete ngay dễ gây inconsistency
> - Sau grace period, `MessageCleanupJob` hard delete thật sự để giải phóng storage

---

## 12. Monitoring & Observability

---

**Q: Observability trong project được implement thế nào?**

> Có 3 pillar của observability:
> 1. **Metrics (Prometheus + Grafana)**: mỗi service expose `/actuator/prometheus`, Prometheus scrape mỗi 15s. Grafana visualize với dashboards: Spring Boot (ID 10280), JVM Micrometer (ID 4701), RabbitMQ (ID 10991). Custom metrics: `notification_sent_total`, `websocket_connections_active`.
> 2. **Logging (ELK Stack)**: service ghi structured JSON logs → Logstash (TCP 5000) → Elasticsearch → Kibana query. Log có `traceId`, `userId`, `service` để correlate across services.
> 3. **Health Checks (Spring Actuator)**: `/actuator/health` check DB connectivity, Redis ping, RabbitMQ, disk space — dùng cho Docker healthcheck và Kubernetes liveness/readiness probe.

---

**Q: Tại sao cần structured logging (JSON) thay vì plain text?**

> Log JSON thay vì text thuần:
> ```json
> { "timestamp": "...", "level": "INFO", "service": "chat-service", "traceId": "abc123", "userId": "usr_01HN...", "message": "Message sent: msg_05..." }
> ```
> - **Machine-parseable**: Logstash/Elasticsearch có thể index từng field riêng
> - **Filter/search**: Kibana query `service:chat-service AND level:ERROR` chạy nhanh
> - **Correlation**: search `traceId:abc123` → thấy toàn bộ request flow qua các services
> Plain text log không thể làm điều này hiệu quả.

---

**Q: Micrometer là gì? Tại sao cần nó thay vì dùng Prometheus trực tiếp?**

> Micrometer là metrics abstraction layer — tương tự SLF4J cho logging. Code chỉ cần dùng Micrometer API (`Counter.increment()`, `Timer.record()`), còn backend (Prometheus, Datadog, InfluxDB...) swap dễ dàng qua configuration. Nếu dùng Prometheus API trực tiếp, sau này muốn switch sang Datadog phải sửa code. Với Micrometer chỉ đổi dependency.

---

## 13. Docker & DevOps

---

**Q: Docker Compose trong project chứa bao nhiêu container?**

> **16 containers** tổng cộng:
> - 6 application services (Gateway, User, Chat, Message Processor, Notification, Scheduled Jobs)
> - 4 databases/infra: PostgreSQL, MongoDB, Redis, RabbitMQ
> - 1 Nginx (reverse proxy)
> - 5 monitoring: Prometheus, Grafana, Elasticsearch, Logstash, Kibana

---

**Q: Dockerfile của services được viết như thế nào?**

> Multi-stage build để giảm image size:
> ```dockerfile
> # Stage 1: Build
> FROM maven:3.9-openjdk-17 AS builder
> WORKDIR /app
> COPY pom.xml .
> RUN mvn dependency:go-offline  # cache dependencies
> COPY src ./src
> RUN mvn package -DskipTests
>
> # Stage 2: Runtime
> FROM openjdk:17-jre-alpine
> COPY --from=builder /app/target/*.jar app.jar
> ENTRYPOINT ["java", "-jar", "app.jar"]
> ```
> Alpine JRE base image nhỏ (~200MB) so với full JDK image (~500MB). Stage 1 chỉ dùng để build, không đưa Maven và source code vào production image.

---

**Q: Nginx trong project làm gì?**

> Nginx đóng vai trò:
> - **Reverse proxy**: nhận traffic từ port 80/443, forward vào API Gateway (8080)
> - **TLS termination**: xử lý HTTPS, downstream là HTTP
> - **Rate limiting**: `limit_req_zone` tại nginx layer (trước khi vào app)
> - **Load balancing**: khi scale API Gateway lên nhiều instance, Nginx cân bằng tải với `least_conn`
> - **Gzip compression**: nén response trước khi trả về client
> - **WebSocket proxy**: transparent pass-through WebSocket connections

---

**Q: Docker health check hoạt động thế nào?**

> Mỗi service trong `docker-compose.yml` có `healthcheck` block:
> ```yaml
> healthcheck:
>   test: ["CMD-SHELL", "pg_isready -U whatsapp"]
>   interval: 10s
>   timeout: 5s
>   retries: 5
> ```
> Docker kiểm tra mỗi 10s, timeout sau 5s, fail 5 lần liên tiếp mới mark container là `unhealthy`. `depends_on: condition: service_healthy` đảm bảo app service không start trước khi DB sẵn sàng.

---

**Q: Environment variable và configuration management trong project thế nào?**

> - **Local dev**: `application.yaml` với default values
> - **Docker**: env vars inject qua `docker-compose.yml` environment section: `SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST`, `JWT_SECRET`, v.v.
> - **Production**: `application-prod.yml` profile với stricter settings
> - **Secret management**: sensitive values (DB passwords, JWT secret, Firebase credentials) qua environment variables, không commit vào code. Trong production dùng Docker Secrets hoặc Kubernetes Secrets.
> - Spring Boot Profiles (`dev`, `docker`, `prod`) cho phép mỗi môi trường có config riêng.

---

## 14. Scalability

---

**Q: Hệ thống này có thể scale thế nào khi load tăng?**

> **Stateless services** (scale dễ): API Gateway, User Service, Message Processor, Notification Service, Scheduled Jobs — chỉ cần `docker-compose --scale user-service=3` hoặc Kubernetes HPA là xong. Không có state local.
>
> **Stateful service** (cần coordination): Chat Service — WebSocket connections là stateful. Cần:
> 1. Nginx sticky sessions theo userId
> 2. Redis Pub/Sub để forward message giữa instances
> 3. Connection registry trong Redis
>
> **Database**: PostgreSQL với read replicas, MongoDB với sharding theo `conversationId`, Redis Cluster.

---

**Q: Horizontal scaling và vertical scaling khác nhau thế nào? Dự án dùng cái nào?**

> - **Vertical**: tăng CPU/RAM của 1 machine — đơn giản nhưng có giới hạn, single point of failure
> - **Horizontal**: thêm nhiều machine, phân tán load — phức tạp hơn nhưng scale không giới hạn, fault tolerant
>
> Dự án thiết kế để **horizontal scale** — stateless services, shared state trong Redis/DB, load balancer trước mỗi tầng. Tất cả services là Spring Boot apps không giữ state local.

---

**Q: Làm thế nào để xử lý database connection pool khi scale lên nhiều instances?**

> HikariCP cấu hình `max-pool-size: 10` per service instance. Nếu scale Chat Service lên 10 replicas → 10 × 10 = 100 connections đến PostgreSQL. PostgreSQL default max connections là 100-200, cần monitor và tune.
>
> Giải pháp khi scale lớn hơn: dùng **PgBouncer** (connection pooler bên ngoài) ngồi giữa app và PostgreSQL, pool ở cấp transaction thay vì session → nhiều app connections share ít DB connections hơn.

---

**Q: Tại sao MongoDB phù hợp để shard hơn PostgreSQL cho message storage?**

> MongoDB được thiết kế cho horizontal sharding từ đầu: shard key `{ conversationId: 1, createdAt: 1 }` phân tán data đều trên các shards. Query theo conversationId luôn hit đúng shard.
>
> PostgreSQL sharding phức tạp hơn nhiều (cần extension như Citus, hoặc application-level sharding). Với message volume hàng tỷ documents, MongoDB native sharding là lựa chọn tự nhiên hơn.

---

## 15. Testing

---

**Q: Strategy testing trong project thế nào?**

> 3 tầng testing:
> 1. **Unit Tests** (JUnit 5 + Mockito): test domain logic (business rules trong `User.create()`, `Message.markAsDelivered()`, `Conversation.addParticipant()`), application services với mock repositories/infrastructure
> 2. **Integration Tests** (Testcontainers + REST Assured): spin up real Docker containers (PostgreSQL, MongoDB, Redis, RabbitMQ), test full flow từ API call đến DB
> 3. **Load Testing**: Apache Bench để benchmark throughput
>
> Maven Surefire chạy unit tests (`**/*Test.java`), Failsafe chạy integration tests (`**/*IT.java`) tách biệt.

---

**Q: Testcontainers là gì? Tại sao dùng nó cho integration test?**

> Testcontainers là library tự động spin up Docker containers trong lúc chạy test. Thay vì cần môi trường có PostgreSQL thật, test tự start `postgres:15-alpine` container, chạy test, rồi cleanup.
>
> Ưu điểm: test chạy trên real database/broker, không dùng H2 in-memory (H2 behaviour khác PostgreSQL về type, constraint, function). CI/CD pipeline không cần pre-installed DB. Nhược điểm: test chậm hơn vì phải pull và start Docker image.

---

## 16. Design Patterns

---

**Q: Trong project này bạn áp dụng những design pattern nào?**

> - **Aggregate Root**: `User`, `Message`, `Conversation` — single entry point, enforce invariants
> - **Repository Pattern**: abstract data access, domain không biết PostgreSQL hay MongoDB
> - **Factory Method**: `User.create()`, `Message.create()` — kiểm soát creation logic
> - **Value Object**: `Email`, `PhoneNumber` — immutable, self-validating
> - **Event Publisher**: `MessageEventPublisher`, `UserEventPublisher` — publish domain events
> - **Strategy Pattern**: message delivery strategy — online (WebSocket trực tiếp) vs offline (Redis inbox + FCM)
> - **Filter Chain**: Gateway Pipeline: Logging → Authentication → RateLimiting → CircuitBreaker → Route
> - **Builder Pattern**: `PushNotification.builder()`, `SendMessageRequest.builder()` — Lombok `@Builder`
> - **Template Method**: `BaseException → BusinessException / ResourceNotFoundException`

---

**Q: Filter Chain trong Gateway khác gì với Interceptor trong Spring MVC?**

> - **Filter** (Servlet Filter / WebFilter): chạy ở tầng thấp nhất, trước khi request vào DispatcherServlet. Có thể short-circuit toàn bộ request (trả về 401 mà không cần Spring MVC xử lý). Spring Cloud Gateway dùng `WebFilter` (reactive).
> - **Interceptor**: chạy trong Spring MVC context, sau khi DispatcherServlet nhận request. Có access vào handler method information.
>
> Gateway dùng `WebFilter` (reactive) phù hợp vì Gateway là reactive application (Netty, WebFlux), không dùng Servlet stack.

---

**Q: Tại sao Repository là interface ở domain layer, implementation ở infrastructure layer?**

> Đây là **Dependency Inversion Principle** trong DDD. Domain chỉ cần biết contract: "Tôi cần `UserRepository.findById(id)` trả về `User`". Domain không biết implementation là JPA, MongoDB hay gọi REST API.
>
> Lợi ích:
> - Unit test domain/application logic dễ dàng với mock repository
> - Swap database không ảnh hưởng business logic (test với H2, production với PostgreSQL)
> - Domain layer thuần Java, không import Spring Data annotations

---

**Q: Câu hỏi cuối: Nếu làm lại dự án, bạn sẽ thay đổi gì?**

> - **Thêm distributed tracing**: integrate Jaeger hoặc Zipkin với OpenTelemetry để trace request flow qua các services, hiện tại chỉ có `traceId` trong log nhưng chưa có visual trace.
> - **GraphQL subscriptions**: thay vì raw WebSocket/STOMP, GraphQL subscription sẽ standardize realtime API hơn.
> - **gRPC** thay REST cho inter-service communication: performance tốt hơn, strongly-typed contracts.
> - **Saga pattern**: xử lý distributed transactions phức tạp hơn (ví dụ: create conversation cần update cả PostgreSQL và MongoDB atomically).
> - **API versioning**: hiện tại không có versioning, sẽ thêm `/api/v1/` prefix cho backward compatibility.

---

*Last updated: February 2026*

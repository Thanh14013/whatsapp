# Chat Service – End-to-End Workflow Documentation

> **Kiến trúc tổng quan:** Domain-Driven Design (DDD) với 4 layer rõ ràng:
> - **Interfaces Layer** – điểm vào của request (REST Controller, WebSocket Controller)
> - **Application Layer** – orchestration (Use Case, Application Service, DTO, Mapper)
> - **Domain Layer** – nghiệp vụ thuần túy (Model, Domain Service, Repository Interface)
> - **Infrastructure Layer** – kỹ thuật (PostgreSQL, MongoDB, Redis, RabbitMQ, WebSocket Session)

---

## Mục lục

1. [Sơ đồ kiến trúc tổng thể](#1-sơ-đồ-kiến-trúc-tổng-thể)
2. [Khởi động ứng dụng](#2-khởi-động-ứng-dụng)
3. [Workflow: Kết nối WebSocket (User Online)](#3-workflow-kết-nối-websocket-user-online)
4. [Workflow: Gửi tin nhắn qua WebSocket](#4-workflow-gửi-tin-nhắn-qua-websocket)
5. [Workflow: Gửi tin nhắn qua REST API](#5-workflow-gửi-tin-nhắn-qua-rest-api)
6. [Workflow: Nhận tin nhắn real-time (RabbitMQ → WebSocket)](#6-workflow-nhận-tin-nhắn-real-time-rabbitmq--websocket)
7. [Workflow: Người nhận offline – Lưu vào inbox cache](#7-workflow-người-nhận-offline--lưu-vào-inbox-cache)
8. [Workflow: User quay lại online – Flush tin nhắn chờ](#8-workflow-user-quay-lại-online--flush-tin-nhắn-chờ)
9. [Workflow: Mark As Delivered (Xác nhận đã nhận)](#9-workflow-mark-as-delivered-xác-nhận-đã-nhận)
10. [Workflow: Mark As Read (Đã đọc)](#10-workflow-mark-as-read-đã-đọc)
11. [Workflow: Tạo cuộc hội thoại (Conversation)](#11-workflow-tạo-cuộc-hội-thoại-conversation)
12. [Workflow: Lấy lịch sử chat (Chat History)](#12-workflow-lấy-lịch-sử-chat-chat-history)
13. [Workflow: Xóa tin nhắn](#13-workflow-xóa-tin-nhắn)
14. [Workflow: Ngắt kết nối WebSocket (User Offline)](#14-workflow-ngắt-kết-nối-websocket-user-offline)
15. [Workflow: Subscribe conversation & query trạng thái online](#15-workflow-subscribe-conversation--query-trạng-thái-online)
16. [Sơ đồ luồng dữ liệu giữa các database](#16-sơ-đồ-luồng-dữ-liệu-giữa-các-database)
17. [Bảng tổng hợp: File – Layer – Trách nhiệm](#17-bảng-tổng-hợp-file--layer--trách-nhiệm)

---

## 1. Sơ đồ kiến trúc tổng thể

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT (Mobile/Web)                        │
│          HTTP REST  ─────────────┐   WebSocket (STOMP/SockJS)       │
└──────────────────────────────────┼──────────────────────────────────┘
                                   │
┌──────────────────────────────────┼──────────────────────────────────┐
│  INTERFACES LAYER                │                                   │
│  ┌─────────────────┐  ┌──────────┴──────────┐  ┌─────────────────┐ │
│  │ConversationCtrl │  │ChatWebSocketCtrl    │  │MessageEventHdlr │ │
│  │MessageController│  │WebSocketHandler     │  │(interfaces/ws)  │ │
│  │(interfaces/rest)│  │(infra/websocket)    │  │                 │ │
│  └────────┬────────┘  └──────────┬──────────┘  └────────┬────────┘ │
└───────────┼───────────────────── ┼────────────────────── ┼─────────┘
            │                      │                        │
┌───────────┼──────────────────────┼────────────────────── ┼─────────┐
│  APPLICATION LAYER               │                        │          │
│  ┌────────┴────────┐  ┌──────────┴──────────┐  ┌────────┴────────┐ │
│  │ConversationSvc  │  │ChatApplicationService│  │MessageQuerySvc  │ │
│  │                 │  │                      │  │                 │ │
│  └────────┬────────┘  └──────────┬──────────┘  └────────┬────────┘ │
│           │            ┌─────────┴─────────┐             │          │
│           │            │   Use Cases        │             │          │
│           │            │ SendMessageUseCase │             │          │
│           │            │ DeliverMsgUseCase  │             │          │
│           │            │ GetChatHistoryUC   │             │          │
│           │            └─────────┬─────────┘             │          │
│           │      MessageMapper   │    ConversationMapper  │          │
└───────────┼─────────────────────┼───────────────────────┼──────────┘
            │                     │                         │
┌───────────┼─────────────────────┼───────────────────────┼──────────┐
│  DOMAIN LAYER                   │                         │          │
│  ┌────────┴────────┐  ┌─────────┴──────────┐  ┌─────────┴───────┐ │
│  │Conversation     │  │Message (Agg.Root)  │  │MessageDomainSvc │ │
│  │(Agg.Root)       │  │MessageStatus       │  │DeliveryTracking │ │
│  │ConversationType │  │MessageType         │  │Service          │ │
│  └────────┬────────┘  └─────────┬──────────┘  └─────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Repository Interfaces (Port)                               │    │
│  │  ConversationRepository    MessageRepository                │    │
│  └────────────────────────────┬────────────────────────────────┘    │
└───────────────────────────────┼────────────────────────────────────┘
                                 │
┌───────────────────────────────┼────────────────────────────────────┐
│  INFRASTRUCTURE LAYER          │                                     │
│  ┌──────────────────┐  ┌───────┴──────────┐  ┌──────────────────┐  │
│  │ConversationRepo  │  │MessageRepositoryI│  │InboxCacheService │  │
│  │Impl (PostgreSQL) │  │mpl (MongoDB)     │  │UndeliveredMsg    │  │
│  │ConversationJpa   │  │MessageMongoRepo  │  │Cache  (Redis)    │  │
│  │Repository        │  │MessageDocument   │  │                  │  │
│  │ConversationEntity│  │                  │  │                  │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │MessageEventPubl. │  │MessageQueueProd. │  │MessageQueueCons. │  │
│  │(RabbitMQ pub)    │  │(RabbitMQ pub)    │  │(RabbitMQ listen) │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │WebSocketSession  │  │ConnectionRegistry│  │SnowflakeIdGen    │  │
│  │Manager           │  │(STOMP lifecycle) │  │                  │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
         │                    │                    │              │
    PostgreSQL             MongoDB               Redis         RabbitMQ
  (Conversations       (Messages –            (Online        (message.exchange
   Participants)        message body)          users,         3 queues)
                                               inbox cache,
                                               conv cache)
```

---

## 2. Khởi động ứng dụng

**File:** `ChatServiceApplication.java` | **Layer:** Entrypoint

```
ChatServiceApplication.main()
  │
  ├─ @SpringBootApplication  → scan toàn bộ bean
  ├─ @EnableJpaAuditing      → tự động set createdAt/updatedAt cho JPA entity
  ├─ @EnableMongoAuditing    → tự động set timestamps cho MongoDB document
  ├─ @EnableCaching          → bật Spring Cache (Redis làm backend)
  └─ @EnableAsync            → cho phép xử lý bất đồng bộ với @Async
```

**Configs được khởi tạo:**

| File | Layer | Làm gì |
|------|-------|--------|
| `WebSocketConfig.java` | infrastructure/config | Đăng ký endpoint `/ws/chat`, prefix `/app`, broker `/topic`, `/queue`, user prefix `/user` |
| `RabbitMQConfig.java` | infrastructure/config | Tạo exchange `message.exchange`, 3 queue: `message.sent.queue`, `message.delivered.queue`, `message.read.queue`, TTL 24h |
| `RedisConfig.java` | infrastructure/config | Kết nối Redis, cấu hình serializer |
| `MongoConfig.java` | infrastructure/config | Cấu hình MongoDB connection |
| `PostgresConfig.java` | infrastructure/config | Cấu hình datasource PostgreSQL, Flyway migration |
| `SnowflakeIdGenerator.java` | infrastructure/idgen | Khởi tạo Snowflake với workerId và datacenterId từ config |

**Database migration (Flyway):**
- `V1__Create_conversations_table.sql` → tạo bảng `conversations`
- `V2__Create_conversation_participants_table.sql` → tạo bảng `conversation_participants`
- `V3__Create_message_delivery_tracking_table.sql` → tạo bảng tracking delivery

---

## 3. Workflow: Kết nối WebSocket (User Online)

**Trigger:** Client mở kết nối STOMP tới `ws://host:8082/ws/chat`

```
Client
  │  STOMP CONNECT (với auth header)
  ▼
WebSocketConfig (infrastructure/config)
  │  endpoint /ws/chat → SockJS fallback
  ▼
Spring STOMP security xác thực → gán Principal (userId)
  │
  ├─► ConnectionRegistry.handleSessionConnected()   [infrastructure/websocket]
  │     │  Đọc Principal → lấy userId, sessionId
  │     ├─ sessionUserMap.put(sessionId, userId)     // lưu mapping local
  │     ├─ WebSocketSessionManager.addSession(userId, sessionId)
  │     │     └─ userSessions: ConcurrentHashMap<userId, Set<sessionId>>
  │     └─ InboxCacheService.addOnlineUser(userId)
  │           └─ Redis SADD "online:users" userId
  │
  └─► WebSocketHandler.onSessionConnected()         [infrastructure/websocket]
        │  @EventListener(SessionConnectedEvent)
        ├─ DeliveryTrackingService.deliverPendingMessages(userId)
        │     │  [domain/service]
        │     ├─ MessageRepository.findUndeliveredMessages(userId)
        │     │     └─ MessageRepositoryImpl → MessageMongoRepository
        │     │           └─ MongoDB query: {receiverId: userId, status: "SENT"}
        │     └─ forEach message: message.markAsDelivered() + messageRepository.save(message)
        │           └─ status: SENT → DELIVERED, deliveredAt = now()
        │
        └─ UndeliveredMessageCache.popAllMessages(userId)
              │  [infrastructure/cache]
              ├─ Redis LRANGE "inbox:{userId}" 0 -1  → lấy tất cả
              ├─ Redis DEL "inbox:{userId}"           → xóa sau khi lấy
              └─ forEach msg: WebSocketSessionManager.sendToUser(userId, "/queue/messages", msg)
                    └─ SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/messages", msg)
                          └─ Client nhận tin nhắn đang chờ qua WebSocket
```

**Kết quả:** User online, toàn bộ tin nhắn chờ được đẩy ngay lập tức.

---

## 4. Workflow: Gửi tin nhắn qua WebSocket

**Trigger:** Client gửi STOMP frame đến `/app/chat.message`

```
Client
  │  STOMP SEND /app/chat.message
  │  Payload: { conversationId, receiverId, content, contentType, replyToMessageId }
  ▼
ChatWebSocketController.sendMessage()              [interfaces/websocket]
  │  @MessageMapping("/chat.message")
  │  Principal → senderId (từ auth)
  │  Build SendMessageRequest (DTO)
  │
  ▼
ChatApplicationService.sendMessage(request)        [application/service]
  │  @Transactional
  │
  ├─ [1] VALIDATE CONVERSATION
  │     ConversationRepository.findById(conversationId)
  │       └─ ConversationRepositoryImpl → ConversationJpaRepository
  │             └─ PostgreSQL SELECT conversations WHERE id = ?
  │     conversation.isParticipant(senderId) → kiểm tra sender có trong nhóm không
  │
  ├─ [2] TẠO MESSAGE DOMAIN OBJECT
  │     MessageContent.text(request.getContent())    [domain/model/vo]
  │     MessageDomainService.createMessage(...)      [domain/service]
  │       │  Validate: senderId ≠ receiverId
  │       └─ Message.create(conversationId, senderId, receiverId, content, replyToId)
  │             │  [domain/model]
  │             ├─ MessageId.generate() → Snowflake ID (64-bit, time-ordered)
  │             ├─ status = MessageStatus.SENT
  │             ├─ sentAt = createdAt = Instant.now()
  │             └─ return new Message(...)
  │
  ├─ [3] LƯU MESSAGE VÀO MONGODB
  │     MessageRepository.save(message)
  │       └─ MessageRepositoryImpl.save()            [infrastructure/persistence/mongodb]
  │             ├─ toDocument(message) → MessageDocument (POJO)
  │             └─ MessageMongoRepository.save(doc)
  │                   └─ MongoDB INSERT vào collection "messages"
  │                         Index: conversation_created_idx, receiver_status_idx
  │
  ├─ [4] CẬP NHẬT CONVERSATION METADATA (PostgreSQL)
  │     conversation.updateLastMessage(messageId, sentAt)
  │       └─ lastMessageId = messageId, lastMessageTimestamp = sentAt
  │     conversation.incrementUnreadCount(receiverId)
  │       └─ unreadCounts[receiverId] += 1
  │     ConversationRepository.save(conversation)
  │       └─ PostgreSQL UPDATE conversations SET last_message_id=?, unread...
  │
  ├─ [5] XÓA CACHE (Redis)
  │     InboxCacheService.evictConversation(conversationId)
  │       └─ Redis DEL "conversation:{conversationId}"
  │
  ├─ [6] PUBLISH EVENT (RabbitMQ)
  │     MessageEventPublisher.publishMessageSent(savedMessage)
  │       └─ RabbitTemplate.convertAndSend(
  │               exchange: "message.exchange",
  │               routingKey: "message.sent",
  │               body: JSON { eventType, messageId, conversationId,
  │                            senderId, receiverId, status, contentType, sentAt }
  │             )
  │
  └─ return MessageDto (qua MessageMapper.toDto())
        └─ MessageMapper.toDto(message) → MessageDto (flat DTO)

  Về lại ChatWebSocketController:
  │  Build MessageReceivedResponse (DTO đầy đủ cho client)
  ├─ Nếu receiver ONLINE: WebSocketSessionManager.sendToUser(receiverId, "/queue/messages", response)
  │     └─ SimpMessagingTemplate → STOMP push tới client receiver
  └─ Echo về sender:       WebSocketSessionManager.sendToUser(senderId, "/queue/messages", response)
        └─ Đồng bộ multi-device của sender
```

**Kết quả:** Message được lưu DB, event được publish, cả sender và receiver (nếu online) nhận được response real-time.

---

## 5. Workflow: Gửi tin nhắn qua REST API

**Trigger:** `POST /api/v1/messages` với JSON body

```
Client
  │  HTTP POST /api/v1/messages
  │  Body: SendMessageRequest JSON
  ▼
MessageController.sendMessage()                    [interfaces/rest]
  │  @PostMapping
  │  @Valid → Bean Validation (NotBlank, Size)
  │  Nếu lỗi validation → ChatExceptionHandler → 422 Unprocessable Entity
  │
  ▼
ChatApplicationService.sendMessage(request)        [application/service]
  │  (Giống hệt bước 2–6 trong Workflow #4)
  │  → Validate → Create Domain Obj → Save MongoDB
  │  → Update PostgreSQL → Evict Redis → Publish RabbitMQ
  │
  └─ return MessageDto
        ▼
MessageController → ResponseEntity.status(201 CREATED).body(messageDto)
  │
  └─ HTTP Response 201 với MessageDto JSON
```

> **Lưu ý:** REST API và WebSocket đều gọi cùng `ChatApplicationService.sendMessage()`. Sự khác biệt: REST không tự push real-time về client – việc đó do RabbitMQ Consumer đảm nhiệm (xem Workflow #6).

---

## 6. Workflow: Nhận tin nhắn real-time (RabbitMQ → WebSocket)

**Trigger:** Sau khi `MessageEventPublisher.publishMessageSent()` publish event vào RabbitMQ

```
RabbitMQ
  │  Queue: "message.sent.queue"
  │  Routing key: "message.sent"
  ▼
MessageQueueConsumer.onMessageSent(payload)        [infrastructure/messaging/consumer]
  │  @RabbitListener(queues = "message.sent.queue")
  │  Parse JSON payload → lấy messageId, receiverId
  │
  ├─ Kiểm tra: WebSocketSessionManager.isUserConnected(receiverId)
  │     └─ ConcurrentHashMap: userSessions.containsKey(receiverId) && not empty
  │
  ├─ [NẾU RECEIVER ONLINE]
  │     WebSocketSessionManager.sendToUser(receiverId, "/queue/messages", event)
  │       └─ SimpMessagingTemplate.convertAndSendToUser(...)
  │             └─ Client receiver nhận message qua WebSocket ngay lập tức
  │     ChatApplicationService.markAsDelivered(messageId, receiverId)
  │       └─ message.markAsDelivered() → status: SENT → DELIVERED
  │       └─ MessageRepository.save() → MongoDB update
  │       └─ MessageEventPublisher.publishMessageDelivered() → RabbitMQ "message.delivered"
  │
  └─ [NẾU RECEIVER OFFLINE]
        Log: "Recipient offline; message queued for later delivery"
        (Message vẫn ở MongoDB với status=SENT, chờ khi user online)
```

**Kết quả:** Receiver nhận được tin nhắn qua WebSocket hoặc tin nhắn chờ trong DB để flush sau.

---

## 7. Workflow: Người nhận offline – Lưu vào inbox cache

> Đây là cơ chế bổ sung. Khi cần lưu `MessageDto` vào Redis inbox để flush nhanh khi user reconnect:

```
[Bất kỳ service nào cần push tin nhắn cho user offline]
  │
  ▼
UndeliveredMessageCache.pushMessage(receiverId, messageDto)   [infrastructure/cache]
  │
  ├─ key = "inbox:{receiverId}"
  ├─ Redis RPUSH "inbox:{receiverId}" <MessageDto JSON>
  └─ Redis EXPIRE "inbox:{receiverId}" 7 ngày
```

**Redis data structure:**
```
Key:   "inbox:user-456"    (List)
Value: [MessageDto1, MessageDto2, ...]
TTL:   7 days
```

**Kết quả:** Tin nhắn được cache trong Redis 7 ngày, sẵn sàng flush khi user online.

---

## 8. Workflow: User quay lại online – Flush tin nhắn chờ

**Trigger:** Client reconnect WebSocket (xem Workflow #3 – đây là chi tiết phần flush)

```
WebSocketHandler.onSessionConnected()              [infrastructure/websocket]
  │
  ├─ [Path A] Flush từ MongoDB (SENT messages)
  │     DeliveryTrackingService.deliverPendingMessages(userId)
  │       ├─ MessageRepository.findUndeliveredMessages(userId)
  │       │     └─ MongoDB query: { receiverId: userId, status: "SENT" } ORDER BY createdAt ASC
  │       └─ forEach message:
  │             message.markAsDelivered()
  │             MessageRepository.save(message) → MongoDB UPDATE status=DELIVERED
  │
  └─ [Path B] Flush từ Redis inbox cache
        UndeliveredMessageCache.popAllMessages(userId)
          ├─ Redis LRANGE "inbox:{userId}" 0 -1    // đọc tất cả
          ├─ Redis DEL "inbox:{userId}"             // xóa atomic
          └─ forEach dto: WebSocketSessionManager.sendToUser(userId, "/queue/messages", dto)
                └─ Push từng tin nhắn tới client qua STOMP
```

**Kết quả:** User nhận đầy đủ tin nhắn bị lỡ trong khi offline.

---

## 9. Workflow: Mark As Delivered (Xác nhận đã nhận)

Có **2 con đường** để mark delivered:

### 9a. Qua WebSocket (STOMP)

```
Client (receiver)
  │  STOMP SEND /app/chat.delivered
  │  Payload: { messageId: "msg-123" }
  ▼
ChatWebSocketController.markDelivered()            [interfaces/websocket]
  │  @MessageMapping("/chat.delivered")
  │  Principal → userId (receiver)
  │
  ▼
ChatApplicationService.markAsDelivered(messageId, userId)   [application/service]
  │  @Transactional
  ├─ MessageRepository.findById(MessageId.of(messageId))
  │     └─ MongoDB findById
  ├─ Validate: message.getReceiverId().equals(userId)
  ├─ message.markAsDelivered()
  │     └─ status: SENT → DELIVERED, deliveredAt = now()
  ├─ MessageRepository.save(updatedMessage) → MongoDB UPDATE
  └─ MessageEventPublisher.publishMessageDelivered(updatedMessage)
        └─ RabbitMQ publish → "message.delivered" routing key

  Về lại ChatWebSocketController:
  └─ WebSocketSessionManager.sendToUser(senderId, "/queue/receipts",
          { type: "DELIVERED", messageId, timestamp })
        └─ Sender nhận delivery receipt real-time
```

### 9b. Qua REST API

```
Client
  │  PUT /api/v1/messages/{id}/delivered?userId=xxx
  ▼
MessageController.markAsDelivered()               [interfaces/rest]
  │
  ▼
ChatApplicationService.markAsDelivered()          [application/service]
  │  (Giống 9a từ bước ChatApplicationService trở đi)
  └─ return MessageDto → HTTP 200 OK
```

### 9c. Tự động khi Consumer nhận message (Workflow #6)

```
MessageQueueConsumer.onMessageSent()  →  ChatApplicationService.markAsDelivered()
  (Được gọi tự động khi receiver online và nhận được push)
```

**Downstream: RabbitMQ Consumer xử lý MESSAGE_DELIVERED:**

```
RabbitMQ
  │  Queue: "message.delivered.queue"
  ▼
MessageQueueConsumer.onMessageDelivered(payload)   [infrastructure/messaging/consumer]
  │  Parse: messageId, senderId
  └─ Nếu sender online: WebSocketSessionManager.sendToUser(senderId, "/queue/receipts", event)
        └─ Sender thấy dấu tích ✓✓ (delivered)
```

---

## 10. Workflow: Mark As Read (Đã đọc)

### 10a. Qua WebSocket

```
Client (receiver)
  │  STOMP SEND /app/chat.read
  │  Payload: { messageId: "msg-123" }
  ▼
ChatWebSocketController.markRead()                 [interfaces/websocket]
  │  @MessageMapping("/chat.read")
  │
  ▼
ChatApplicationService.markAsRead(messageId, userId)    [application/service]
  │  @Transactional
  ├─ MessageRepository.findById(...)  →  MongoDB
  ├─ Validate: receiver là người gọi
  ├─ message.markAsRead()
  │     └─ status → READ, readAt = now()
  │        (nếu chưa DELIVERED: tự set deliveredAt = readAt)
  ├─ MessageRepository.save() → MongoDB UPDATE
  ├─ ConversationRepository.findById() → PostgreSQL
  ├─ conversation.resetUnreadCount(userId)
  │     └─ unreadCounts[userId] = 0
  ├─ ConversationRepository.save() → PostgreSQL UPDATE
  ├─ InboxCacheService.evictConversation(conversationId)
  │     └─ Redis DEL "conversation:{id}"
  └─ MessageEventPublisher.publishMessageRead(updatedMessage)
        └─ RabbitMQ "message.read" routing key

  Về lại ChatWebSocketController:
  └─ sendToUser(senderId, "/queue/receipts", { type: "READ", messageId, timestamp })
```

### 10b. Qua WebSocket /app/chat.markRead (đánh dấu cả conversation)

```
Client
  │  STOMP SEND /app/chat.markRead
  │  Payload: { conversationId, userId }
  ▼
WebSocketHandler.handleMarkRead()                  [infrastructure/websocket]
  │
  ▼
ChatApplicationService.markAsRead(conversationId, userId)
  │  (Thực ra gọi ConversationService.markAsRead() thông qua chatService)
  └─ conversation.resetUnreadCount(userId) → PostgreSQL save → Redis evict
```

**Downstream: RabbitMQ Consumer xử lý MESSAGE_READ:**

```
RabbitMQ Queue: "message.read.queue"
  ▼
MessageQueueConsumer.onMessageRead()
  └─ Nếu sender online: sendToUser(senderId, "/queue/receipts", event)
        └─ Sender thấy dấu tích ✓✓ màu xanh (read)
```

---

## 11. Workflow: Tạo cuộc hội thoại (Conversation)

**Trigger:** `POST /api/v1/conversations`

```
Client
  │  HTTP POST /api/v1/conversations
  │  Body: CreateConversationRequest { type, participant1Id, participant2Id, ... }
  ▼
ConversationController.createConversation()        [interfaces/rest]
  │  @PostMapping, @Valid
  │
  ▼
ConversationService.createConversation(request)    [application/service]
  │
  ├─ [NẾU ONE_TO_ONE]
  │     ConversationRepository.findOneToOneConversation(p1Id, p2Id)
  │       └─ PostgreSQL query JOIN participants WHERE user1 AND user2
  │     Nếu đã tồn tại → return DTO luôn (idempotent)
  │     Nếu chưa:
  │       Conversation.createOneToOne(p1Id, p1Name, p2Id, p2Name)
  │         ├─ ConversationId.generate() → Snowflake ID
  │         ├─ type = ONE_TO_ONE
  │         ├─ participants = [Participant(p1), Participant(p2)]
  │         ├─ unreadCounts = { p1: 0, p2: 0 }
  │         └─ active = true
  │
  └─ [NẾU GROUP]
        additionalParticipants.map → List<Participant>
        Conversation.createGroup(name, desc, creatorId, creatorName, participants)
          ├─ ConversationId.generate()
          ├─ creator → Participant.createAdmin() (isAdmin = true)
          └─ unreadCounts = { mỗi participant: 0 }

  ConversationRepository.save(conversation)
    └─ ConversationRepositoryImpl.save()           [infrastructure/persistence/postgres]
          ├─ toEntity(conversation) → ConversationEntity + List<ConversationParticipantEntity>
          │     └─ Đặt back-reference: participantEntity.setConversation(entity)
          └─ ConversationJpaRepository.save(entity)
                └─ PostgreSQL INSERT INTO conversations + conversation_participants

  return ConversationMapper.toDto(savedConversation) → HTTP 201 CREATED
```

---

## 12. Workflow: Lấy lịch sử chat (Chat History)

### 12a. Qua REST API

```
Client
  │  GET /api/v1/messages/conversation/{conversationId}?page=0&size=50
  ▼
MessageController.getConversationMessages()        [interfaces/rest]
  │
  ▼
ChatApplicationService.getConversationMessages(conversationId, page, size)   [application/service]
  │
  ▼
MessageRepository.findByConversationId(ConversationId.of(id), page, size)
  └─ MessageRepositoryImpl                         [infrastructure/persistence/mongodb]
        ├─ page = offset / size
        └─ MessageMongoRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId,
                PageRequest.of(page, size, Sort.by(DESC, "createdAt"))
              )
              └─ MongoDB: db.messages.find({conversationId: ?})
                           .sort({createdAt: -1}).skip(page*size).limit(size)

  List<Message> → stream().map(MessageMapper::toDto) → List<MessageDto>
  └─ HTTP 200 OK, JSON array
```

### 12b. Qua WebSocket (subscribe conversation)

```
Client
  │  STOMP SEND /app/chat.subscribe
  │  Payload: { conversationId, page: 0, size: 50 }
  ▼
MessageEventHandler.handleSubscribe()              [interfaces/websocket]
  │  @MessageMapping("/chat.subscribe")
  │
  ▼
MessageQueryService.getConversationHistory(conversationId, page, size)   [application/service]
  │  (CQRS – read-only service, không dùng ChatApplicationService)
  └─ MessageRepository.findByConversationId(...)
        └─ MongoDB query (giống 12a)

  sessionManager.sendToUser(userId, "/queue/history",
      { conversationId, page, size, messages: List<MessageDto> })
  └─ Client nhận lịch sử qua WebSocket
```

### 12c. Qua Use Case (dùng từ các component khác)

```
GetChatHistoryUseCase.execute(conversationId, page, size)   [application/usecase]
  │  Giới hạn size <= 100 (MAX_PAGE_SIZE)
  └─ ChatApplicationService.getConversationMessages(...)
        └─ (Giống 12a)
```

---

## 13. Workflow: Xóa tin nhắn

**Business Rule:** Chỉ sender mới được xóa; chỉ trong vòng 1 giờ sau khi gửi.

```
Client
  │  DELETE /api/v1/messages/{id}?userId=xxx
  ▼
MessageController.deleteMessage()                  [interfaces/rest]
  │
  ▼
ChatApplicationService.deleteMessage(messageId, userId)     [application/service]
  │  @Transactional
  ├─ MessageRepository.findById(MessageId.of(messageId))  →  MongoDB
  ├─ Validate: message.getSenderId().equals(userId)
  │     Nếu không → throw IllegalArgumentException → 400 Bad Request
  ├─ message.delete(userId)                        [domain/model]
  │     ├─ Kiểm tra: userId == senderId
  │     ├─ Kiểm tra: createdAt > now - 1 giờ
  │     │     Nếu quá 1h → throw IllegalStateException → 409 Conflict
  │     ├─ deleted = true
  │     └─ deletedAt = Instant.now()
  └─ MessageRepository.save(message)  →  MongoDB UPDATE (soft delete)

HTTP 204 No Content
```

---

## 14. Workflow: Ngắt kết nối WebSocket (User Offline)

**Trigger:** Client đóng tab, mất mạng, hoặc timeout

```
Spring WebSocket
  │  SessionDisconnectEvent
  ▼
ConnectionRegistry.handleSessionDisconnected()     [infrastructure/websocket]
  │  @EventListener(SessionDisconnectEvent)
  ├─ accessor.getSessionId() → sessionId
  ├─ userId = sessionUserMap.remove(sessionId)
  ├─ WebSocketSessionManager.removeSession(userId, sessionId)
  │     └─ userSessions[userId].remove(sessionId)
  │        Nếu set rỗng → userSessions.remove(userId) hoàn toàn
  └─ Nếu !sessionManager.isUserConnected(userId):
        InboxCacheService.removeOnlineUser(userId)
          └─ Redis SREM "online:users" userId
```

**Kết quả:** User bị đánh dấu offline. Tin nhắn gửi đến sẽ không được push WebSocket mà chờ trong MongoDB (status=SENT).

---

## 15. Workflow: Subscribe conversation & query trạng thái online

### 15a. Query trạng thái online của user khác

```
Client
  │  STOMP SEND /app/chat.status
  │  Payload: { targetUserId: "user-456" }
  ▼
MessageEventHandler.handleStatusQuery()            [interfaces/websocket]
  │  @MessageMapping("/chat.status")
  ├─ sessionManager.isUserConnected(targetUserId)
  │     └─ userSessions.containsKey(targetUserId) && !empty
  └─ sessionManager.sendToUser(requesterId, "/queue/status",
          { userId: targetUserId, online: true/false, timestamp })
```

### 15b. Query số tin nhắn chưa đọc (badge count)

```
Client
  │  STOMP SEND /app/chat.unreadCount
  │  Payload: { userId: "user-123" }
  ▼
MessageEventHandler.handleUnreadCount()            [interfaces/websocket]
  │
  ▼
MessageQueryService.countUndelivered(userId)       [application/service]
  └─ MessageRepository.countUndeliveredMessages(userId)
        └─ MessageMongoRepository.countByReceiverIdAndStatus(userId, "SENT")
              └─ MongoDB: db.messages.count({receiverId: userId, status: "SENT"})

  sendToUser(userId, "/queue/unreadCount", { unreadCount: N, timestamp })
```

### 15c. Inbox sync thủ công khi reconnect

```
Client
  │  STOMP SEND /app/chat.sync
  │  Payload: { userId: "user-123" }
  ▼
WebSocketHandler.handleSync()                      [infrastructure/websocket]
  │  @MessageMapping("/chat.sync")
  └─ UndeliveredMessageCache.popAllMessages(userId)
        ├─ Redis LRANGE + DEL "inbox:{userId}"
        └─ forEach msg: sessionManager.sendToUser(userId, "/queue/messages", msg)
```

---

## 16. Sơ đồ luồng dữ liệu giữa các database

```
┌────────────────────────────────────────────────────────────────┐
│                        SEND MESSAGE                            │
│                                                                │
│  MongoDB              PostgreSQL              Redis            │
│  ─────────            ──────────              ─────            │
│  INSERT message       UPDATE conversation     DEL cache        │
│  (content, status,    (lastMessageId,         "conversation:id"│
│   senderId,           lastTimestamp,                           │
│   receiverId,         unreadCount++)                           │
│   conversationId)                                              │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                       MARK AS READ                             │
│                                                                │
│  MongoDB              PostgreSQL              Redis            │
│  ─────────            ──────────              ─────            │
│  UPDATE message       UPDATE conversation     DEL cache        │
│  status=READ          unreadCount[user]=0     "conversation:id"│
│  readAt=now()                                                  │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                     USER COMES ONLINE                          │
│                                                                │
│  MongoDB              PostgreSQL              Redis            │
│  ─────────            ──────────              ─────────────    │
│  SELECT status=SENT   (không đọc)             SADD             │
│  UPDATE status=       (không ghi)             "online:users"   │
│  DELIVERED                                    userId           │
│                                               LRANGE+DEL       │
│                                               "inbox:{userId}" │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                        DATA OWNERSHIP                          │
│                                                                │
│  PostgreSQL                   MongoDB                          │
│  ──────────                   ───────                          │
│  conversations                messages                         │
│  conversation_participants    (nội dung tin nhắn)              │
│  (metadata & relationships)   (scalable, schema-flexible)      │
│                                                                │
│  Redis (temporary / cache)    RabbitMQ (events)                │
│  ──────────────────────────   ─────────────────                │
│  online:users (Set)           message.exchange (Topic)         │
│  conversation:{id} (String)   message.sent.queue               │
│  user:conversations:{id}(Set) message.delivered.queue          │
│  inbox:{userId} (List)        message.read.queue               │
└────────────────────────────────────────────────────────────────┘
```

---

## 17. Bảng tổng hợp: File – Layer – Trách nhiệm

### Interfaces Layer (Điểm vào)

| File | Endpoint / Trigger | Gọi ai | Làm gì |
|------|--------------------|--------|--------|
| `ConversationController.java` | `POST/GET /api/v1/conversations` | `ConversationService` | CRUD conversation qua REST |
| `MessageController.java` | `POST/GET/PUT/DELETE /api/v1/messages` | `ChatApplicationService` | CRUD message qua REST |
| `ChatWebSocketController.java` | STOMP `/app/chat.message`, `/app/chat.delivered`, `/app/chat.read` | `ChatApplicationService`, `WebSocketSessionManager` | Gửi/acknowledge message qua WebSocket |
| `MessageEventHandler.java` | STOMP `/app/chat.subscribe`, `/app/chat.status`, `/app/chat.unreadCount` | `MessageQueryService`, `WebSocketSessionManager` | Query lịch sử, status, unread count qua WebSocket |
| `ChatExceptionHandler.java` | `@RestControllerAdvice` | — | Bắt exception toàn cục → RFC 7807 Problem Detail response |

### Application Layer (Orchestration)

| File | Kiểu | Gọi ai | Làm gì |
|------|------|--------|--------|
| `ChatApplicationService.java` | Service | `MessageRepository`, `ConversationRepository`, `MessageDomainService`, `InboxCacheService`, `MessageEventPublisher`, `MessageMapper` | Orchestrate toàn bộ message lifecycle: send, delivered, read, delete, query |
| `ConversationService.java` | Service | `ConversationRepository`, `InboxCacheService`, `ConversationMapper` | CRUD conversation, quản lý participants |
| `MessageQueryService.java` | Service (CQRS read) | `MessageRepository`, `MessageMapper` | Chỉ đọc: history, undelivered, count |
| `SendMessageUseCase.java` | Use Case | `ChatApplicationService` | Wrapper rõ ràng cho use case "gửi tin nhắn" |
| `DeliverMessageUseCase.java` | Use Case | `ChatApplicationService` | Wrapper cho use case "mark as delivered" |
| `GetChatHistoryUseCase.java` | Use Case | `ChatApplicationService` | Wrapper + giới hạn page size <= 100 |
| `MessageMapper.java` | Mapper | — | Domain `Message` → `MessageDto` |
| `ConversationMapper.java` | Mapper | — | Domain `Conversation` → `ConversationDto` |
| `SendMessageRequest.java` | DTO | — | Input DTO từ client (REST/WebSocket) |
| `MessageDto.java` | DTO | — | Output DTO cho message |
| `ConversationDto.java` | DTO | — | Output DTO cho conversation |
| `MessageReceivedResponse.java` | DTO | — | Real-time push DTO qua WebSocket |

### Domain Layer (Nghiệp vụ thuần túy)

| File | Kiểu | Làm gì |
|------|------|--------|
| `Message.java` | Aggregate Root | Model tin nhắn; factory `create()`, lifecycle: `markAsDelivered()`, `markAsRead()`, `delete()` |
| `Conversation.java` | Aggregate Root | Model cuộc hội thoại; factory `createOneToOne()`, `createGroup()`; quản lý participants, unread counts |
| `MessageStatus.java` | Enum | `SENT`, `DELIVERED`, `READ` |
| `MessageType.java` | Enum | Loại tin nhắn |
| `ConversationType.java` | Enum | `ONE_TO_ONE`, `GROUP` |
| `MessageId.java` | Value Object | Snowflake ID cho message |
| `ConversationId.java` | Value Object | Snowflake ID cho conversation |
| `MessageContent.java` | Value Object | Nội dung tin nhắn + type (TEXT/IMAGE/...) |
| `Participant.java` | Value Object | Thành viên hội thoại (userId, displayName, isAdmin) |
| `MessageDomainService.java` | Domain Service | Validate và tạo Message object; validate content length |
| `DeliveryTrackingService.java` | Domain Service | Bulk-deliver pending messages; validate status transitions; check delivered/read |
| `MessageRepository.java` | Interface (Port) | Contract lưu/đọc Message (impl ở MongoDB) |
| `ConversationRepository.java` | Interface (Port) | Contract lưu/đọc Conversation (impl ở PostgreSQL) |

### Infrastructure Layer (Kỹ thuật)

| File | Công nghệ | Làm gì |
|------|-----------|--------|
| `MessageRepositoryImpl.java` | MongoDB | Implement `MessageRepository`; map `Message` ↔ `MessageDocument`; dùng `MessageMongoRepository` |
| `MessageMongoRepository.java` | Spring Data MongoDB | Query MongoDB: history, undelivered, count |
| `MessageDocument.java` | MongoDB Document | Schema lưu message trong collection "messages" |
| `ConversationRepositoryImpl.java` | PostgreSQL/JPA | Implement `ConversationRepository`; map `Conversation` ↔ `ConversationEntity` |
| `ConversationJpaRepository.java` | Spring Data JPA | Query PostgreSQL: findByParticipant, findOneToOne |
| `ConversationEntity.java` | JPA Entity | Schema bảng `conversations` |
| `ConversationParticipantEntity.java` | JPA Entity | Schema bảng `conversation_participants` |
| `MessageEventPublisher.java` | RabbitMQ | Publish 3 event types: `MESSAGE_SENT`, `MESSAGE_DELIVERED`, `MESSAGE_READ` |
| `MessageQueueProducer.java` | RabbitMQ | Low-level publish helper (serialize + send tới routing key) |
| `MessageQueueConsumer.java` | RabbitMQ | Listen 3 queue; push qua WebSocket nếu user online; auto mark-delivered |
| `WebSocketSessionManager.java` | STOMP/Spring | Map userId ↔ sessionIds; `sendToUser()`, `broadcast()`, `isUserConnected()` |
| `ConnectionRegistry.java` | STOMP Events | Lắng nghe `SessionConnectedEvent`, `SessionDisconnectEvent`; đồng bộ session map + Redis online set |
| `WebSocketHandler.java` | STOMP | Handle `/app/chat.sendMessage`, `/app/chat.markRead`, `/app/chat.sync`; flush offline messages khi connect |
| `InboxCacheService.java` | Redis | Cache conversation data; quản lý online users set (`online:users`) |
| `UndeliveredMessageCache.java` | Redis | Per-user inbox list (`inbox:{userId}`); push/pop/peek MessageDto, TTL 7 ngày |
| `SnowflakeIdGenerator.java` | Hutool Snowflake | Tạo distributed unique ID (64-bit, time-ordered) |
| `WebSocketConfig.java` | Spring WebSocket | Config STOMP: endpoint `/ws/chat`, prefix `/app`, broker `/topic /queue /user` |
| `RabbitMQConfig.java` | RabbitMQ | Declare exchange + 3 queue + bindings + TTL 24h |
| `RedisConfig.java` | Redis | Config RedisTemplate + serializer |
| `MongoConfig.java` | MongoDB | Config MongoDB connection |
| `PostgresConfig.java` | PostgreSQL | Config datasource + Flyway |

---

## Tóm tắt luồng chính theo sequence

### Gửi tin nhắn (Happy path – receiver online)

```
Client A                 Server                              Client B
   │                        │                                    │
   │──STOMP /chat.message──►│                                    │
   │                        │ ChatWebSocketController            │
   │                        │ → ChatApplicationService           │
   │                        │   → MessageDomainService           │
   │                        │     → Message.create()             │
   │                        │   → MongoDB INSERT (status=SENT)   │
   │                        │   → PostgreSQL UPDATE conv         │
   │                        │   → Redis DEL cache                │
   │                        │   → RabbitMQ PUBLISH message.sent  │
   │                        │ → sendToUser(B, /queue/messages)   │
   │◄─/queue/messages(echo)─│                                    │
   │                        │────/queue/messages────────────────►│
   │                        │                                    │
   │                        │ MessageQueueConsumer               │
   │                        │ onMessageSent()                    │
   │                        │ → markAsDelivered()                │
   │                        │   → MongoDB UPDATE (DELIVERED)     │
   │                        │   → RabbitMQ PUBLISH delivered     │
   │                        │ onMessageDelivered()               │
   │◄─/queue/receipts(✓✓)──│                                    │
   │                        │                                    │
   │                    Client B đọc tin                         │
   │                        │◄─STOMP /chat.read─────────────────│
   │                        │ → markAsRead()                     │
   │                        │   → MongoDB UPDATE (READ)          │
   │                        │   → PostgreSQL resetUnreadCount    │
   │                        │   → RabbitMQ PUBLISH read          │
   │◄─/queue/receipts(✓✓✓)─│                                    │
```

### Gửi tin nhắn khi receiver offline

```
Client A                 Server                          [Later] Client B
   │                        │                                    │
   │──REST POST /messages──►│                                    │
   │                        │ → MongoDB INSERT (status=SENT)     │
   │                        │ → RabbitMQ PUBLISH message.sent    │
   │◄──201 Created──────────│                                    │
   │                        │ MessageQueueConsumer               │
   │                        │ onMessageSent():                   │
   │                        │   isUserConnected(B) = false       │
   │                        │   → message stays SENT in MongoDB  │
   │                        │                                    │
   │                        │         [B connects later]         │
   │                        │◄────────STOMP CONNECT──────────────│
   │                        │ ConnectionRegistry.onConnect()     │
   │                        │ → Redis SADD online:users B        │
   │                        │ WebSocketHandler.onConnect()       │
   │                        │ → deliverPendingMessages(B)        │
   │                        │   → MongoDB: status SENT→DELIVERED │
   │                        │ → popAllMessages(B) from Redis     │
   │                        │────/queue/messages────────────────►│
```


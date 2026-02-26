# 📋 Notification Service — End-to-End Workflow Documentation

> **Mục đích:** Tài liệu này mô tả **toàn bộ luồng hoạt động (workflow) từ đầu đến cuối** của Notification Service trong hệ thống WhatsApp Clone.  
> Mỗi bước đều chỉ rõ: **file nào → ở layer nào → gọi hàm/service nào → làm gì → để làm gì**.

---

## 🗂️ Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Cấu trúc thư mục & vai trò từng file](#2-cấu-trúc-thư-mục--vai-trò-từng-file)
3. [Khởi động ứng dụng (Startup)](#3-workflow-khởi-động-ứng-dụng-startup)
4. [Workflow 1: Đăng ký Device Token](#4-workflow-1-đăng-ký-device-token)
5. [Workflow 2: Gửi Push Notification khi có tin nhắn mới (Event-Driven)](#5-workflow-2-gửi-push-notification-khi-có-tin-nhắn-mới-event-driven)
6. [Workflow 3: Gửi Notification thủ công qua REST API](#6-workflow-3-gửi-notification-thủ-công-qua-rest-api)
7. [Workflow 4: Xóa Device Token](#7-workflow-4-xóa-device-token)
8. [Workflow 5: Scheduled Tasks (tự động bảo trì)](#8-workflow-5-scheduled-tasks-tự-động-bảo-trì)
9. [Workflow 6: Xử lý lỗi toàn cục (Exception Handling)](#9-workflow-6-xử-lý-lỗi-toàn-cục-exception-handling)
10. [Sơ đồ luồng tổng thể](#10-sơ-đồ-luồng-tổng-thể)
11. [Sơ đồ phụ thuộc giữa các component](#11-sơ-đồ-phụ-thuộc-giữa-các-component)
12. [Các khái niệm kỹ thuật quan trọng](#12-các-khái-niệm-kỹ-thuật-quan-trọng)

---

## 1. Tổng quan kiến trúc

```
┌─────────────────────────────────────────────────────────────────┐
│                      NOTIFICATION SERVICE                        │
│                                                                   │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐   │
│  │RabbitMQ  │───▶│Consumer  │    │REST API  │    │Scheduler │   │
│  │(events)  │    │(listener)│    │(HTTP)    │    │(cron)    │   │
│  └──────────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘   │
│                       │               │               │          │
│                       ▼               ▼               │          │
│              ┌────────────────────────────┐           │          │
│              │      NotificationService   │           │          │
│              │   (Orchestration layer)    │           │          │
│              └────────┬─────────┬─────────┘           │          │
│                       │         │                      │          │
│             ┌─────────▼──┐  ┌───▼──────────┐          │          │
│             │ FCMService │  │DeviceToken   │◀─────────┘          │
│             │ (Firebase) │  │Service       │                     │
│             └─────────┬──┘  └───┬──────────┘                    │
│                       │         │                                 │
│            ┌──────────▼──┐  ┌───▼──────────┐                    │
│            │ Firebase    │  │  Redis       │                    │
│            │ FCM/APNS    │  │  (cache)     │                    │
│            └─────────────┘  └──────────────┘                    │
└─────────────────────────────────────────────────────────────────┘
```

### Các thành phần bên ngoài giao tiếp với service:

| Thành phần bên ngoài | Vai trò |
|---|---|
| **RabbitMQ** | Message broker — các service khác (Message Service) gửi event vào đây, Notification Service lắng nghe và xử lý |
| **Redis** | Cache — lưu trữ device tokens của user, tra cứu nhanh khi cần gửi notification |
| **Firebase FCM** | Cloud platform của Google — nhận notification từ server và đẩy đến thiết bị Android/iOS/Web |
| **APNS** | Apple Push Notification Service — gửi notification đến iOS (hiện tại là placeholder, chưa implement thật) |

---

## 2. Cấu trúc thư mục & vai trò từng file

```
notification/
├── NotificationServiceApplication.java     ← Điểm khởi động ứng dụng
│
├── config/                                  ← LAYER CẤU HÌNH (chạy 1 lần lúc startup)
│   ├── AsyncConfig.java                     ← Cấu hình Thread Pool cho async
│   ├── FirebaseConfig.java                  ← Khởi tạo kết nối Firebase
│   ├── NotificationProperties.java          ← Map config từ application.yml vào Java object
│   ├── NotificationRabbitMQConfig.java      ← Khai báo queue RabbitMQ
│   └── NotificationRedisConfig.java         ← Cấu hình kết nối Redis
│
├── consumer/                                ← LAYER TIÊU THỤ SỰ KIỆN (event consumer)
│   └── NotificationConsumer.java            ← Lắng nghe RabbitMQ, xử lý event
│
├── controller/                              ← LAYER HTTP API (nhận HTTP request)
│   └── NotificationController.java          ← REST endpoints cho notification
│
├── domain/model/                            ← LAYER DOMAIN (dữ liệu thuần túy)
│   ├── DeviceToken.java                     ← Model đại diện cho 1 device token
│   └── PushNotification.java                ← Model đại diện cho 1 push notification
│
├── dto/                                     ← LAYER DTO (data transfer objects)
│   ├── RegisterTokenRequest.java            ← Request body để đăng ký token
│   └── SendNotificationRequest.java         ← Request body để gửi notification thủ công
│
├── exception/                               ← LAYER XỬ LÝ LỖI
│   └── NotificationExceptionHandler.java    ← Global exception handler
│
├── repository/                              ← LAYER REPOSITORY (interface)
│   └── DeviceTokenRepository.java           ← Interface định nghĩa contract với Redis
│
├── scheduler/                               ← LAYER SCHEDULER (tác vụ định kỳ)
│   └── NotificationScheduler.java           ← Scheduled jobs bảo trì hệ thống
│
└── service/                                 ← LAYER SERVICE (business logic)
    ├── APNSNotificationService.java         ← Gửi notification đến iOS (placeholder)
    ├── DeviceTokenService.java              ← Quản lý device tokens trong Redis
    ├── FCMService.java                      ← Gửi notification qua Firebase FCM
    ├── NotificationMetrics.java             ← Ghi metrics cho Prometheus/Grafana
    └── NotificationService.java             ← Orchestrator: điều phối toàn bộ quá trình gửi
```

### Phân tầng rõ ràng:

```
HTTP Request  ──▶  Controller  ──▶  Service  ──▶  FCMService / Redis
RabbitMQ Event ──▶  Consumer   ──▶  Service  ──▶  FCMService / Redis
Scheduler      ──▶  (trực tiếp)──▶  Redis
```

---

## 3. Workflow: Khởi động ứng dụng (Startup)

Khi Spring Boot khởi động, các bean được tạo theo thứ tự:

```
[1] NotificationServiceApplication.java
        ↓  main() → SpringApplication.run()
        ↓  Spring container bắt đầu scan & tạo beans

[2] Config beans (layer config) — chạy trước tiên:

    [2a] NotificationRedisConfig.java
         → redisConnectionFactory()   tạo kết nối Lettuce đến Redis (localhost:6379, db=2)
         → redisTemplate()            tạo RedisTemplate<String,String> dùng StringRedisSerializer

    [2b] FirebaseConfig.java
         → firebaseApp()              đọc file firebase-credentials.json
                                      khởi tạo FirebaseApp với GoogleCredentials
         → firebaseMessaging()        tạo FirebaseMessaging bean từ FirebaseApp

    [2c] NotificationRabbitMQConfig.java
         → messageSentQueue()         khai báo queue "message.sent" (durable=true)
         → userStatusChangedQueue()   khai báo queue "user.status.changed" (durable=true)
         → messageConverter()         cấu hình Jackson2JsonMessageConverter (JSON ↔ Object)
         → rabbitListenerContainerFactory()  tạo factory với 3-5 concurrent consumers

    [2d] AsyncConfig.java
         → notificationExecutor()     tạo ThreadPoolTaskExecutor
                                      core=5, max=10, queue=100, prefix="notification-"

    [2e] NotificationProperties.java
         → @ConfigurationProperties("app.notification")
           map config TTL, priority, sound từ application.yml vào Java object

[3] Service beans:
    DeviceTokenService   (inject RedisTemplate)
    FCMService           (inject FirebaseMessaging)
    APNSNotificationService
    NotificationMetrics  (inject MeterRegistry → tạo các Counter, Timer cho Prometheus)
    NotificationService  (inject DeviceTokenService, FCMService, APNSNotificationService, NotificationMetrics)

[4] Consumer & Controller:
    NotificationConsumer   (inject NotificationService)
    NotificationController (inject NotificationService, DeviceTokenService)
    NotificationScheduler  (inject RedisTemplate)
    NotificationExceptionHandler

[5] RabbitMQ listeners bắt đầu lắng nghe:
    - queue "message.sent"         → NotificationConsumer.handleMessageSent()
    - queue "user.status.changed"  → NotificationConsumer.handleUserStatusChanged()

✅ Ứng dụng sẵn sàng nhận request tại port 8084, context-path /api/v1
```

---

## 4. Workflow 1: Đăng ký Device Token

> **Kịch bản:** User A mở app WhatsApp trên điện thoại. App lấy FCM token từ Firebase SDK và gửi lên server để đăng ký.

### Luồng chi tiết:

```
CLIENT (Mobile App)
    │
    │  POST /api/v1/notifications/register
    │  Body: { "userId": "user-123", "token": "fcm-abc...", "platform": "ANDROID" }
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ LAYER: Controller                                                │
│ File: NotificationController.java                               │
│                                                                  │
│ @PostMapping("/register")                                        │
│ registerToken(RegisterTokenRequest request)                      │
│   │                                                              │
│   ├─ @Valid: validate request (userId, token không được rỗng)   │
│   │         nếu lỗi → ExceptionHandler trả 400 Bad Request      │
│   │                                                              │
│   └─ gọi: deviceTokenService.registerToken(userId, token, platform)
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ LAYER: Service                                                   │
│ File: DeviceTokenService.java                                    │
│                                                                  │
│ registerToken(userId, token, platform)                          │
│   │                                                              │
│   ├─ Tạo key Redis:                                             │
│   │    tokenKey      = "device:token:<token>"                   │
│   │    userTokensKey = "user:tokens:<userId>"                   │
│   │                                                              │
│   ├─ redisTemplate.opsForValue().set(tokenKey, userId, 90 days)  │
│   │    → Lưu mapping: token → userId (để sau có thể tra userId) │
│   │                                                              │
│   └─ redisTemplate.opsForSet().add(userTokensKey, token)        │
│      redisTemplate.expire(userTokensKey, 90 days)               │
│        → Thêm token vào set của user (1 user có thể có nhiều   │
│          token = nhiều thiết bị)                                │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ INFRASTRUCTURE: Redis (database: 2)                             │
│                                                                  │
│ Stored data:                                                     │
│   KEY: "device:token:fcm-abc..."  VALUE: "user-123"  TTL: 90d   │
│   KEY: "user:tokens:user-123"     TYPE: SET                     │
│                   VALUES: {"fcm-abc...", "fcm-xyz..."}           │
│                   TTL: 90 days                                   │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
Controller trả về:
    HTTP 200 OK
    { "success": true, "message": "Device token registered successfully" }
```

### Dữ liệu Redis sau khi đăng ký:

| Redis Key | Kiểu | Giá trị | TTL |
|---|---|---|---|
| `device:token:fcm-abc...` | String | `user-123` | 90 ngày |
| `user:tokens:user-123` | Set | `{fcm-abc..., fcm-xyz...}` | 90 ngày |

---

## 5. Workflow 2: Gửi Push Notification khi có tin nhắn mới (Event-Driven)

> **Kịch bản:** User A gửi tin nhắn cho User B. Message Service xử lý xong và publish event `MESSAGE_SENT` vào RabbitMQ. Notification Service nhận event và đẩy push notification đến các thiết bị của User B.

### Đây là luồng **quan trọng nhất** và **tự động** nhất của service.

```
MESSAGE SERVICE (service khác trong hệ thống)
    │
    │  Publish message vào RabbitMQ:
    │  Queue: "message.sent"
    │  Payload (JSON):
    │  {
    │    "eventType": "MESSAGE_SENT",
    │    "messageId": "msg-001",
    │    "senderId": "user-A",
    │    "receiverId": "user-B",
    │    "senderName": "Nguyen Van A",
    │    "content": "Xin chào bạn!"
    │  }
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ INFRASTRUCTURE: RabbitMQ                                        │
│                                                                  │
│  Queue: "message.sent" (durable, survives restart)              │
│  Consumer: 3 concurrent listeners (max 5), prefetch: 10        │
│  Ack mode: auto (tự động ack sau khi xử lý)                    │
└─────────────────────────────────────────────────────────────────┘
    │  (RabbitMQ giao message cho listener)
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ LAYER: Consumer                                                 │
│ File: NotificationConsumer.java                                 │
│                                                                  │
│ @RabbitListener(queues = "message.sent")                        │
│ handleMessageSent(String messageJson)                            │
│   │                                                              │
│   ├─ objectMapper.readValue(messageJson, Map.class)             │
│   │    → Parse JSON string thành Map<String, Object>            │
│   │                                                              │
│   ├─ Kiểm tra eventType == "MESSAGE_SENT"                       │
│   │    → Nếu không đúng: log warning, return (bỏ qua)          │
│   │                                                              │
│   ├─ Lấy các field:                                             │
│   │    messageId  = event.get("messageId")  → "msg-001"        │
│   │    senderId   = event.get("senderId")   → "user-A"         │
│   │    receiverId = event.get("receiverId") → "user-B"         │
│   │    senderName = event.get("senderName") → "Nguyen Van A"   │
│   │    content    = event.get("content")    → "Xin chào bạn!"  │
│   │                                                              │
│   ├─ Truncate content: nếu > 100 ký tự thì cắt + "..."         │
│   │    preview = "Xin chào bạn!"  (OK, < 100 ký tự)            │
│   │                                                              │
│   └─ gọi: notificationService.sendMessageNotification(          │
│               receiverId="user-B",                              │
│               senderId="user-A",                                │
│               senderName="Nguyen Van A",                        │
│               messagePreview="Xin chào bạn!")                   │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ LAYER: Service                                                  │
│ File: NotificationService.java                                  │
│                                                                  │
│ sendMessageNotification(userId, senderId, senderName, preview)  │
│   │                                                              │
│   ├─ Tạo data map:                                              │
│   │    { "type": "message",                                      │
│   │      "senderId": "user-A",                                  │
│   │      "senderName": "Nguyen Van A" }                         │
│   │                                                              │
│   ├─ title = "New message from Nguyen Van A"                    │
│   ├─ body  = "Xin chào bạn!"                                    │
│   │                                                              │
│   └─ gọi: sendNotification(userId="user-B", title, body, data)  │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ LAYER: Service                                                  │
│ File: NotificationService.java                                  │
│                                                                  │
│ @Async  ← QUAN TRỌNG: method này chạy trong thread pool riêng  │
│          (không block thread đang xử lý message từ RabbitMQ)   │
│                                                                  │
│ sendNotification(userId, title, body, data)                     │
│   │                                                              │
│   ├─ [BƯỚC 1] Lấy tất cả device tokens của user-B:             │
│   │    gọi: deviceTokenService.getTokensForUser("user-B")       │
│   │      → Trả về Set: {"fcm-token-phone", "fcm-token-tablet"}  │
│   │                                                              │
│   ├─ Nếu Set rỗng:                                              │
│   │    log "No device tokens found"                             │
│   │    gọi: notificationMetrics.recordNotificationSkipped()     │
│   │    return (dừng, không làm gì thêm)                         │
│   │                                                              │
│   ├─ [BƯỚC 2] Tạo PushNotification object:                     │
│   │    PushNotification {                                        │
│   │      id:        UUID.randomUUID()                           │
│   │      userId:    "user-B"                                    │
│   │      type:      MESSAGE                                      │
│   │      title:     "New message from Nguyen Van A"             │
│   │      body:      "Xin chào bạn!"                             │
│   │      data:      {type, senderId, senderName}                │
│   │      priority:  HIGH                                         │
│   │      ttl:       86400 (24 giờ)                              │
│   │      createdAt: now()                                        │
│   │      status:    PENDING                                      │
│   │    }                                                         │
│   │                                                              │
│   └─ [BƯỚC 3] Lặp qua từng token, gọi sendToToken(token, notif)│
└─────────────────────────────────────────────────────────────────┘
    │  (lặp qua mỗi token)
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ LAYER: Service (private method)                                 │
│ File: NotificationService.java                                  │
│                                                                  │
│ sendToToken(token, notification)                                │
│   │                                                              │
│   └─ gọi: fcmService.sendNotification(token, notification)      │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ LAYER: Service                                                  │
│ File: FCMService.java                                           │
│                                                                  │
│ sendNotification(token, notification)                           │
│   │                                                              │
│   ├─ buildMessage(token, notification):                         │
│   │    ┌─ setToken(token)           ← gắn FCM device token      │
│   │    ├─ setNotification(          ← title + body              │
│   │    │     Notification.builder()                              │
│   │    │       .setTitle("New message from...")                 │
│   │    │       .setBody("Xin chào bạn!")                        │
│   │    │       .build())                                         │
│   │    ├─ putAllData({type, senderId, senderName})              │
│   │    ├─ setAndroidConfig(          ← cấu hình riêng Android   │
│   │    │     priority: HIGH                                      │
│   │    │     sound: "default"                                    │
│   │    │     ttl: 86400000ms)                                    │
│   │    ├─ setApnsConfig(             ← cấu hình riêng iOS       │
│   │    │     aps.alert.title + body                              │
│   │    │     aps.sound: "default")                               │
│   │    └─ setWebpushConfig(          ← cấu hình riêng Web       │
│   │          title + body)                                       │
│   │                                                              │
│   └─ firebaseMessaging.send(message)                            │
│        → Gọi Firebase API, trả về messageId                     │
│        → Nếu thành công: return messageId                       │
│        → Nếu lỗi (FirebaseMessagingException):                  │
│              handleFCMException() → log error                    │
│              deviceTokenService.removeToken(token)  ← token hết│
│              return null                                         │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ INFRASTRUCTURE: Firebase Cloud Messaging (Google)               │
│                                                                  │
│  FCM nhận message → đẩy đến thiết bị Android/iOS/Web           │
│  của user-B theo token đã cung cấp                              │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ Sau khi gửi hết tất cả tokens:                                  │
│ File: NotificationService.java                                  │
│                                                                  │
│  log: "Notification sent: 2 successful out of 2 devices"       │
│  gọi: notificationMetrics.recordNotificationSent()              │
│        → tăng Counter "notification_sent_total" +1              │
│        → Prometheus có thể scrape metric này                    │
└─────────────────────────────────────────────────────────────────┘

📱 KẾT QUẢ: User B nhận được push notification trên điện thoại:
   ┌──────────────────────────────┐
   │ 🔔 New message from Nguyen Van A │
   │ Xin chào bạn!                │
   └──────────────────────────────┘
```

---

## 6. Workflow 3: Gửi Notification thủ công qua REST API

> **Kịch bản:** Admin hoặc developer muốn test bằng cách gửi notification thủ công, không qua RabbitMQ.

```
CLIENT (Postman / Admin tool)
    │
    │  POST /api/v1/notifications/send
    │  Body: { "userId": "user-B", "senderName": "Test", "message": "Hello test!" }
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│ LAYER: Controller                                               │
│ File: NotificationController.java                               │
│                                                                  │
│ @PostMapping("/send")                                           │
│ sendNotification(SendNotificationRequest request)               │
│   │                                                              │
│   └─ gọi: notificationService.sendMessageNotification(          │
│               userId, senderName, message)                       │
│            ← Lưu ý: method này chỉ có 3 tham số,               │
│              senderId được truyền là senderName                 │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
  [Tiếp tục giống Workflow 2 từ NotificationService trở đi]
    │
    ▼
  HTTP 200 OK: { "success": true, "message": "Notification sent successfully" }
```

---

## 7. Workflow 4: Xóa Device Token

### 4a. Xóa 1 token cụ thể

```
CLIENT
    │  DELETE /api/v1/notifications/token/{fcm-token-abc}
    ▼
NotificationController.removeToken(token)
    │
    └─▶ DeviceTokenService.removeToken(token)
            │
            ├─ redisTemplate.opsForValue().get("device:token:fcm-token-abc")
            │    → Lấy userId = "user-123"
            │
            ├─ redisTemplate.opsForSet().remove("user:tokens:user-123", token)
            │    → Xóa token khỏi set của user
            │
            └─ redisTemplate.delete("device:token:fcm-token-abc")
                 → Xóa mapping token → userId

  HTTP 200 OK
```

### 4b. Xóa tất cả token của 1 user (khi logout)

```
CLIENT
    │  DELETE /api/v1/notifications/user/{userId}/tokens
    ▼
NotificationController.removeAllTokens(userId)
    │
    └─▶ DeviceTokenService.removeAllTokensForUser(userId)
            │
            ├─ getTokensForUser(userId)
            │    → Lấy toàn bộ tokens của user từ Redis Set
            │
            ├─ Lặp qua từng token:
            │    redisTemplate.delete("device:token:<token>")
            │
            └─ redisTemplate.delete("user:tokens:<userId>")
                 → Xóa cả Set của user

  HTTP 200 OK
```

> **Tại sao cần xóa token khi logout?**  
> Nếu không xóa, server vẫn sẽ cố gửi notification đến thiết bị đó dù user đã logout. FCM sẽ trả về lỗi token invalid, gây lãng phí tài nguyên.

---

## 8. Workflow 5: Scheduled Tasks (tự động bảo trì)

> File: `NotificationScheduler.java` — chạy định kỳ **không cần trigger từ bên ngoài**

```
┌─────────────────────────────────────────────────────────────────┐
│ LAYER: Scheduler                                                │
│ File: NotificationScheduler.java                                │
│                                                                  │
│ ┌──────────────────────────────────────────────────────────┐    │
│ │ Task 1: cleanupExpiredTokens()                           │    │
│ │ @Scheduled(fixedRate = 21600000) — chạy mỗi 6 giờ       │    │
│ │                                                           │    │
│ │  → Đếm số keys "device:token:*" trong Redis             │    │
│ │  → Log: "Current device tokens in Redis: 1234"          │    │
│ │  → Ghi chú: Redis tự xóa key hết TTL, scheduler này    │    │
│ │    chỉ để monitoring (có thể mở rộng thêm logic)        │    │
│ └──────────────────────────────────────────────────────────┘    │
│                                                                  │
│ ┌──────────────────────────────────────────────────────────┐    │
│ │ Task 2: generateStatistics()                             │    │
│ │ @Scheduled(fixedRate = 3600000) — chạy mỗi 1 giờ        │    │
│ │                                                           │    │
│ │  → countDeviceTokens(): đếm keys "device:token:*"       │    │
│ │  → countUsers(): đếm keys "user:tokens:*"               │    │
│ │  → Log: "Statistics - Total tokens: X, Total users: Y"  │    │
│ └──────────────────────────────────────────────────────────┘    │
│                                                                  │
│ ┌──────────────────────────────────────────────────────────┐    │
│ │ Task 3: healthCheck()                                    │    │
│ │ @Scheduled(fixedRate = 300000) — chạy mỗi 5 phút        │    │
│ │                                                           │    │
│ │  → redisTemplate.getConnectionFactory()                 │    │
│ │      .getConnection().ping()                             │    │
│ │  → Nếu OK: log "Health check passed"                    │    │
│ │  → Nếu lỗi: log ERROR "Health check failed"             │    │
│ └──────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. Workflow 6: Xử lý lỗi toàn cục (Exception Handling)

> File: `NotificationExceptionHandler.java` — `@RestControllerAdvice` bắt mọi exception từ Controller

```
Có 3 loại lỗi được xử lý:

┌──────────────────────────────────────────────────────────────────┐
│ [1] MethodArgumentNotValidException                              │
│     → Xảy ra khi: @Valid kiểm tra thất bại (field rỗng, null)   │
│     → Ví dụ: POST /register mà không có userId                  │
│     → Xử lý:                                                     │
│         Lặp qua các lỗi, tạo Map<fieldName, errorMessage>       │
│         Trả về: HTTP 400 Bad Request                             │
│         Body: { "success": false,                                │
│                 "error": { "code": "VALIDATION_ERROR",          │
│                            "details": {"userId": "required"} }} │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ [2] IllegalArgumentException                                     │
│     → Xảy ra khi: business logic phát hiện argument không hợp lệ│
│     → Xử lý:                                                     │
│         Trả về: HTTP 400 Bad Request                             │
│         Body: { "success": false, "message": ex.getMessage() }  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ [3] Exception (catch-all)                                        │
│     → Xảy ra khi: bất kỳ exception nào không được bắt ở trên   │
│     → Xử lý:                                                     │
│         Log ERROR với stack trace                                │
│         Trả về: HTTP 500 Internal Server Error                   │
│         Body: { "success": false,                                │
│                 "message": "An unexpected error occurred" }      │
└──────────────────────────────────────────────────────────────────┘

⚠️  Lưu ý: Lỗi trong Consumer (RabbitMQ) và Scheduler KHÔNG đi qua
    ExceptionHandler này vì chúng không phải HTTP request.
    Chúng được xử lý bằng try-catch nội bộ trong từng file.
```

---

## 10. Sơ đồ luồng tổng thể

```
                        ┌──────────────────────────────────────────────┐
                        │          EXTERNAL SYSTEMS                    │
                        │                                              │
                        │  [Message Service] ──publish──▶ [RabbitMQ]  │
                        │  [Mobile App]      ──HTTP──▶    [REST API]  │
                        └──────────────────────────────────────────────┘
                                   │                    │
                       ┌───────────▼──┐       ┌────────▼──────────┐
                       │   Consumer   │       │    Controller      │
                       │  Layer       │       │    Layer           │
                       │              │       │                    │
                       │handleMessage │       │registerToken()     │
                       │  Sent()      │       │removeToken()       │
                       │handleUser    │       │getUserTokens()     │
                       │  Status      │       │sendNotification()  │
                       │  Changed()   │       │                    │
                       └──────┬───────┘       └────────┬──────────┘
                              │                        │
                              └──────────┬─────────────┘
                                         │
                              ┌──────────▼──────────────┐
                              │     Service Layer        │
                              │                          │
                              │   NotificationService    │◀──── @Async
                              │   (Orchestrator)         │      (Thread Pool)
                              └──────┬──────────┬────────┘
                                     │          │
                         ┌───────────▼──┐  ┌────▼──────────────┐
                         │  FCMService  │  │ DeviceTokenService │
                         │              │  │                    │
                         │sendNotif()   │  │registerToken()     │
                         │sendBatch()   │  │getTokensForUser()  │
                         │sendToTopic() │  │removeToken()       │
                         └──────┬───────┘  └────────┬──────────┘
                                │                   │
                    ┌───────────▼────┐   ┌──────────▼──────────┐
                    │ Firebase FCM   │   │       Redis          │
                    │ (Google Cloud) │   │  (device tokens)     │
                    │                │   │  db=2                │
                    │ → Push đến     │   │  TTL=90 days         │
                    │   Android/     │   │                      │
                    │   iOS/Web      │   │                      │
                    └────────────────┘   └─────────────────────┘

              ┌────────────────────────────────────────────────────┐
              │              Background Jobs                       │
              │                                                    │
              │  NotificationScheduler                             │
              │    ├── cleanupExpiredTokens() → every 6h          │
              │    ├── generateStatistics()   → every 1h          │
              │    └── healthCheck()          → every 5min        │
              └────────────────────────────────────────────────────┘

              ┌────────────────────────────────────────────────────┐
              │              Observability                         │
              │                                                    │
              │  NotificationMetrics → MeterRegistry → Prometheus │
              │    ├── notification_sent_total (Counter)           │
              │    ├── notification_failed_total (Counter)         │
              │    ├── device_token_registered_total (Counter)     │
              │    └── notification_send_duration (Timer)          │
              │                                                    │
              │  Actuator endpoints:                               │
              │    /actuator/health, /actuator/metrics,           │
              │    /actuator/prometheus                            │
              └────────────────────────────────────────────────────┘
```

---

## 11. Sơ đồ phụ thuộc giữa các component

```
NotificationServiceApplication
    └── @EnableAsync → sử dụng AsyncConfig.notificationExecutor()
    └── @EnableScheduling → kích hoạt NotificationScheduler

NotificationController
    ├── depends on → NotificationService
    └── depends on → DeviceTokenService

NotificationConsumer
    └── depends on → NotificationService
                     ObjectMapper (Jackson)

NotificationService
    ├── depends on → DeviceTokenService
    ├── depends on → FCMService
    ├── depends on → APNSNotificationService
    └── depends on → NotificationMetrics

FCMService
    └── depends on → FirebaseMessaging (bean từ FirebaseConfig)

DeviceTokenService
    └── depends on → RedisTemplate<String,String> (bean từ NotificationRedisConfig)

NotificationScheduler
    └── depends on → RedisTemplate<String,String>

NotificationMetrics
    └── depends on → MeterRegistry (Spring Boot Actuator tự tạo)

FirebaseConfig
    └── tạo ra → FirebaseApp, FirebaseMessaging

NotificationRedisConfig
    └── tạo ra → RedisConnectionFactory, RedisTemplate

NotificationRabbitMQConfig
    └── tạo ra → Queue "message.sent", Queue "user.status.changed"
                 MessageConverter, RabbitListenerContainerFactory

AsyncConfig
    └── tạo ra → ThreadPoolTaskExecutor "notificationExecutor"
```

---

## 12. Các khái niệm kỹ thuật quan trọng

### 🔑 Device Token là gì?
Mỗi khi user cài app và mở lần đầu, Firebase SDK tự tạo ra 1 chuỗi ký tự duy nhất gọi là **FCM Token** (hay APNS Token với iOS). Token này đại diện cho "địa chỉ" để gửi notification đến thiết bị đó. Server phải lưu token này lại để sau có thể push notification.

### 🔑 Tại sao dùng Redis để lưu token thay vì Database?
- Redis là **in-memory** → tra cứu cực nhanh (O(1))
- Hỗ trợ **TTL** tự động xóa dữ liệu hết hạn (token hết hạn sau 90 ngày)
- Hỗ trợ **Set** data structure → 1 user có thể có nhiều token (nhiều thiết bị), thêm/xóa/lấy dễ dàng

### 🔑 Tại sao dùng @Async khi gửi notification?
Khi Consumer nhận event từ RabbitMQ, nếu gửi notification đồng bộ (sync), nó sẽ block thread đang xử lý message. `@Async` giúp tách riêng việc gửi notification vào một thread pool khác (`notificationExecutor`), Consumer có thể tiếp tục nhận event mới ngay lập tức.

### 🔑 FCM vs APNS
| | FCM | APNS |
|---|---|---|
| Nền tảng | Android + iOS + Web | iOS only |
| Provider | Google Firebase | Apple |
| Trong code | FCMService.java (implement đầy đủ) | APNSNotificationService.java (placeholder, chưa implement) |
| Ghi chú | FCM cũng hỗ trợ iOS qua APNs bridge, nên hiện tại toàn bộ dùng FCM | |

### 🔑 Event-Driven Architecture
Notification Service **không biết** khi nào tin nhắn được gửi. Nó chỉ lắng nghe sự kiện từ RabbitMQ. Thiết kế này:
- **Loose coupling**: Notification Service độc lập hoàn toàn với Message Service
- **Scalable**: Có thể scale riêng lẻ từng service
- **Resilient**: Nếu Notification Service down, RabbitMQ giữ message lại, khi service restart sẽ xử lý tiếp

### 🔑 Redis Data Structure cho Device Tokens

```
Redis (database 2):

1. Mapping token → userId (dùng để xóa token biết userId là ai):
   Key:   "device:token:<FCM_TOKEN>"
   Type:  String
   Value: "<USER_ID>"
   TTL:   90 days

2. Set các tokens của 1 user (dùng để gửi notification đến tất cả thiết bị):
   Key:   "user:tokens:<USER_ID>"
   Type:  Set
   Value: { "<FCM_TOKEN_1>", "<FCM_TOKEN_2>", "<FCM_TOKEN_3>" }
   TTL:   90 days
```

### 🔑 Metrics & Observability
Service expose metrics qua `/actuator/prometheus` cho hệ thống monitoring (Prometheus + Grafana) có thể scrape:
- `notification_sent_total` — tổng số notification gửi thành công
- `notification_failed_total` — tổng số notification thất bại
- `device_token_registered_total` — tổng số token đã đăng ký
- `notification_send_duration` — thời gian trung bình gửi 1 notification

### 🔑 Queue Configuration
| Queue | Durable | Concurrent Consumers | Max Consumers | Prefetch |
|---|---|---|---|---|
| `message.sent` | ✅ Yes (tồn tại sau restart) | 3 | 5 | 10 |
| `user.status.changed` | ✅ Yes | 3 | 5 | 10 |

---

## 📌 Tóm tắt nhanh: "Ai làm gì?"

| File | Nhiệm vụ chính |
|---|---|
| `NotificationServiceApplication` | Điểm khởi động, bật Async & Scheduling |
| `AsyncConfig` | Tạo thread pool 5-10 threads cho async notification |
| `FirebaseConfig` | Kết nối Firebase bằng service account credentials |
| `NotificationRedisConfig` | Kết nối Redis, tạo RedisTemplate |
| `NotificationRabbitMQConfig` | Khai báo queues, cấu hình JSON converter |
| `NotificationProperties` | Map config TTL/priority/sound từ YAML vào Java |
| `NotificationConsumer` | Lắng nghe RabbitMQ, parse event JSON, gọi NotificationService |
| `NotificationController` | Expose REST API để đăng ký token, xóa token, gửi notification thủ công |
| `NotificationService` | **Orchestrator**: điều phối lấy token → tạo notification object → gửi đến từng device |
| `DeviceTokenService` | CRUD token trong Redis (register, get, remove) |
| `FCMService` | Build FCM message với cấu hình Android/iOS/Web, gọi Firebase API |
| `APNSNotificationService` | Placeholder cho APNS (chưa implement, hiện trả về true) |
| `NotificationMetrics` | Ghi counter/timer cho Prometheus |
| `NotificationScheduler` | 3 cron jobs: cleanup, statistics, health check |
| `NotificationExceptionHandler` | Bắt exception toàn cục, trả lỗi chuẩn JSON |
| `DeviceToken` | Domain model: userId, token, platform, timestamps, active |
| `PushNotification` | Domain model: id, userId, type, title, body, data, priority, ttl, status |
| `RegisterTokenRequest` | DTO validate input đăng ký token (userId, token, platform) |
| `SendNotificationRequest` | DTO validate input gửi notification thủ công (userId, senderName, message) |
| `DeviceTokenRepository` | Interface contract cho Redis operations (chưa có implementation class riêng, DeviceTokenService làm luôn) |


# WhatsApp Clone - Notification Service

## 📋 Mô tả

Notification Service xử lý push notifications cho offline users thông qua Firebase Cloud Messaging (FCM).

## 🎯 Tính năng

### Core Features
- ✅ **Push Notifications**: Gửi notifications qua FCM
- ✅ **Device Token Management**: Quản lý device tokens trong Redis
- ✅ **Multi-Platform Support**: Android, iOS, Web
- ✅ **Batch Notifications**: Gửi hàng loạt notifications
- ✅ **Notification Types**: Message, Typing, System

### Notification Types
- **MESSAGE**: New message notifications (high priority)
- **TYPING**: Typing indicator (normal priority, 10s TTL)
- **SYSTEM**: System notifications (normal priority)

## 🏗️ Kiến trúc

```
notification-service/
├── domain/
│   └── model/
│       ├── DeviceToken.java
│       └── PushNotification.java
├── service/
│   ├── FCMService.java              # Firebase integration
│   ├── DeviceTokenService.java      # Token management
│   └── NotificationService.java     # Main service
├── controller/
│   └── NotificationController.java  # REST API
├── config/
│   └── FirebaseConfig.java          # Firebase setup
└── dto/
    ├── RegisterTokenRequest.java
    └── SendNotificationRequest.java
```

## 🚀 Quick Start

### Prerequisites
- Firebase project with FCM enabled
- Firebase service account JSON file

### Firebase Setup

1. Create Firebase project: https://console.firebase.google.com
2. Generate service account key:
    - Project Settings → Service Accounts
    - Generate New Private Key
    - Save JSON file

3. Configure credentials:
```bash
export FIREBASE_CREDENTIALS_PATH=/path/to/firebase-credentials.json
```

### Build & Run

```bash
# Build
mvn clean package -pl notification-service -am

# Run locally
mvn spring-boot:run -pl notification-service

# Build Docker image
docker build -t whatsapp-clone/notification-service:latest \
  -f notification-service/Dockerfile .

# Run with Docker
docker run -d \
  --name notification-service \
  -p 8084:8084 \
  -e FIREBASE_CREDENTIALS_PATH=/app/config/firebase-credentials.json \
  -v /path/to/firebase-credentials.json:/app/config/firebase-credentials.json \
  whatsapp-clone/notification-service:latest
```

## 📡 API Endpoints

### Register Device Token
```bash
POST /notifications/register
Content-Type: application/json

{
  "userId": "user-123",
  "token": "fcm-device-token",
  "platform": "ANDROID",
  "deviceName": "Samsung Galaxy S21"
}
```

### Send Notification (Manual)
```bash
POST /notifications/send
Content-Type: application/json

{
  "userId": "user-123",
  "senderName": "John Doe",
  "message": "Hello!"
}
```

### Get User Tokens
```bash
GET /notifications/tokens/{userId}
```

### Remove Device Token
```bash
DELETE /notifications/token/{token}
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase credentials JSON | - |
| `FIREBASE_DATABASE_URL` | Firebase database URL (optional) | - |
| `SPRING_DATA_REDIS_HOST` | Redis hostname | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |

### application.yml

```yaml
firebase:
  credentials:
    path: ${FIREBASE_CREDENTIALS_PATH}

app:
  notification:
    message:
      ttl: 3600      # 1 hour
      priority: HIGH
    typing:
      ttl: 10        # 10 seconds
      priority: NORMAL
```

## 📊 Notification Flow

```
1. User goes offline
2. Message arrives
3. Message Processor Service detects offline user
4. Calls Notification Service
5. Notification Service:
   - Gets device tokens from Redis
   - Sends push notification via FCM
   - Updates token last used timestamp
   - Removes invalid tokens
```

## 🔍 Monitoring

### Health Check
```bash
curl http://localhost:8084/actuator/health
```

### Metrics
```bash
curl http://localhost:8084/actuator/prometheus
```

## 📝 FCM Message Format

### Android
```json
{
  "notification": {
    "title": "John Doe",
    "body": "Hello!",
    "sound": "default"
  },
  "data": {
    "type": "message",
    "sender": "John Doe",
    "preview": "Hello!",
    "timestamp": "2026-02-13T10:00:00Z"
  },
  "android": {
    "priority": "high",
    "ttl": "3600s"
  }
}
```

### iOS (APNS)
```json
{
  "notification": {
    "title": "John Doe",
    "body": "Hello!"
  },
  "apns": {
    "payload": {
      "aps": {
        "alert": {
          "title": "John Doe",
          "body": "Hello!"
        },
        "sound": "default"
      }
    }
  }
}
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Firebase credentials not found
```bash
# Check file path
ls -la $FIREBASE_CREDENTIALS_PATH

# Verify JSON format
cat $FIREBASE_CREDENTIALS_PATH | jq .
```

#### 2. Device token not registered
```bash
# Check Redis
redis-cli
> GET device:token:{token}
> SMEMBERS user:tokens:{userId}
```

#### 3. FCM quota exceeded
- Check Firebase console for quota limits
- Implement rate limiting
- Use batch sending for multiple recipients

## 🔐 Security

### Best Practices
- Store Firebase credentials securely
- Never commit credentials to Git
- Use environment variables for sensitive data
- Rotate device tokens periodically (90 days TTL)
- Validate all input data
- Rate limit API endpoints

## 📈 Performance

### Optimization Tips
- Use batch sending for multiple devices
- Cache device tokens in Redis (90 days TTL)
- Async notification sending
- Remove invalid tokens automatically
- Use appropriate TTL for different notification types

## 🧪 Testing

### Unit Tests
```bash
mvn test -pl notification-service
```

### Integration Tests
```bash
mvn verify -pl notification-service
```

### Test Notification
```bash
curl -X POST http://localhost:8084/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "senderName": "Test Sender",
    "message": "Test notification"
  }'
```

## 📚 References

- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [FCM HTTP v1 API](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages)
- [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)

## 📝 License

MIT License
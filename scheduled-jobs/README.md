# WhatsApp Clone - Scheduled Jobs Service

## 📋 Mô tả

Scheduled Jobs Service xử lý các tác vụ định kỳ và dọn dẹp dữ liệu cho hệ thống WhatsApp Clone.

## 🎯 Tính năng

### Core Jobs

#### 1️⃣ **Message Cleanup Jobs**
- **Undelivered Messages**: Xóa tin nhắn chưa gửi > 1 năm
    - Schedule: Daily 2:00 AM
    - Retention: 365 days

- **Delivered Messages**: Xóa tin nhắn đã gửi theo policy
    - Schedule: Daily 3:00 AM
    - Default retention: 90 days

- **Deleted Messages**: Xóa tin nhắn đã xóa sau grace period
    - Schedule: Daily 4:00 AM
    - Grace period: 30 days

- **Message Statistics**: Tạo thống kê tin nhắn
    - Schedule: Daily 5:00 AM

#### 2️⃣ **Cache Cleanup Jobs**
- **Orphaned Inbox Entries**: Dọn inbox entries rỗng
    - Schedule: Hourly

- **Expired Device Tokens**: Dọn tokens hết hạn
    - Schedule: Every 6 hours

- **Stale User Status**: Dọn status entries cũ
    - Schedule: Hourly

- **Cache Statistics**: Tạo thống kê cache
    - Schedule: Hourly

- **Temporary Caches**: Xóa cache tạm
    - Schedule: Daily 1:00 AM

#### 3️⃣ **User Policy Cleanup Jobs**
- **User Retention Policies**: Enforce user-specific policies
    - Schedule: Daily 6:00 AM
    - 0-day policy: Xóa ngay
    - 90-day policy: Xóa sau 90 ngày

- **Inactive Users**: Đánh dấu users không hoạt động
    - Schedule: Weekly (Sunday 7:00 AM)
    - Threshold: 6 months

- **Deactivated Accounts**: Xóa tài khoản đã deactivate
    - Schedule: Daily 8:00 AM
    - Deletion delay: 30 days

- **User Statistics**: Tạo thống kê users
    - Schedule: Daily 9:00 AM

- **Expired Sessions**: Xóa sessions hết hạn
    - Schedule: Every 6 hours
    - Expiry: 7 days

## 🏗️ Kiến trúc

```
scheduled-jobs-service/
├── jobs/
│   ├── MessageCleanupJob.java
│   ├── CacheCleanupJob.java
│   └── UserPolicyCleanupJob.java
├── config/
│   └── SchedulerConfig.java
└── SchedulerApplication.java
```

## 📅 Job Schedule

| Job | Schedule | Description |
|-----|----------|-------------|
| Undelivered Messages Cleanup | Daily 2:00 AM | Delete > 1 year |
| Delivered Messages Cleanup | Daily 3:00 AM | Delete > 90 days |
| Deleted Messages Cleanup | Daily 4:00 AM | Delete after 30 days |
| Message Statistics | Daily 5:00 AM | Generate stats |
| User Policy Enforcement | Daily 6:00 AM | Enforce retention |
| Inactive User Cleanup | Weekly Sun 7:00 AM | Mark inactive |
| Deactivated Account Cleanup | Daily 8:00 AM | Delete accounts |
| User Statistics | Daily 9:00 AM | Generate stats |
| Orphaned Inbox Cleanup | Hourly | Remove empty inboxes |
| Cache Statistics | Hourly | Generate cache stats |
| Temporary Cache Cleanup | Daily 1:00 AM | Clear temp caches |
| Expired Device Tokens | Every 6 hours | Clean tokens |
| Stale User Status | Hourly | Clean status |
| Expired Sessions | Every 6 hours | Clean sessions |

## 🚀 Quick Start

### Build & Run

```bash
# Build
mvn clean package -pl scheduled-jobs-service -am

# Run locally
mvn spring-boot:run -pl scheduled-jobs-service

# Build Docker image
docker build -t whatsapp-clone/scheduled-jobs:latest \
  -f scheduled-jobs-service/Dockerfile .

# Run with Docker
docker run -d \
  --name scheduled-jobs \
  -p 8085:8085 \
  whatsapp-clone/scheduled-jobs:latest
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL URL | `jdbc:postgresql://localhost:5432/whatsapp` |
| `SPRING_DATA_MONGODB_URI` | MongoDB URI | `mongodb://localhost:27017/whatsapp` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |

### application.yml

```yaml
app:
  scheduler:
    message-cleanup:
      undelivered-retention-days: 365
      delivered-retention-days: 90
      deleted-grace-period-days: 30
    
    user-policy:
      inactive-threshold-days: 180
      deactivated-deletion-days: 30
      session-expiry-days: 7
```

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8085/actuator/health
```

### Scheduled Tasks Info
```bash
curl http://localhost:8085/actuator/scheduledtasks
```

### Metrics
```bash
curl http://localhost:8085/actuator/prometheus
```

## 🔍 Cleanup Details

### Message Cleanup

**Undelivered Messages:**
```
Criteria: undelivered = true AND createdAt < (now - 365 days)
Action: DELETE from messages collection
Result: Permanent deletion
```

**Delivered Messages:**
```
Criteria: undelivered != true AND createdAt < (now - 90 days)
Action: DELETE from messages collection
Result: Permanent deletion based on retention policy
```

**Deleted Messages:**
```
Criteria: deleted = true AND deletedAt < (now - 30 days)
Action: DELETE from messages collection
Result: Permanent deletion after grace period
```

### Cache Cleanup

**Orphaned Inbox:**
```
Pattern: inbox:*
Criteria: Set size = 0
Action: DELETE key
```

**Device Tokens:**
```
Pattern: device:token:*
Criteria: TTL expired (90 days)
Action: Automatic Redis expiration
```

### User Policy

**Inactive Users:**
```
Criteria: last_seen_at < (now - 180 days) AND active = true
Action: UPDATE users SET active = false
```

**Deactivated Accounts:**
```
Criteria: active = false AND updated_at < (now - 30 days)
Action: DELETE user and associated data
```

## 📈 Statistics

Jobs generate statistics for:
- Total messages in database
- Undelivered vs delivered messages
- Deleted messages count
- Active vs inactive users
- Cache entry counts by type

Statistics are logged and can be exported to monitoring systems.

## ⚠️ Important Notes

### Data Retention
- **Undelivered**: 1 year (requirement)
- **Delivered**: 0-90 days (user configurable)
- **Deleted**: 30-day grace period

### Performance
- Jobs use batch processing (1000 records)
- Run during off-peak hours (1:00 AM - 9:00 AM)
- Thread pool: 10 concurrent tasks
- Graceful shutdown: 60 seconds wait

### Safety
- All deletions are permanent
- No soft delete after grace period
- Logs all cleanup operations
- Error handling prevents data corruption

## 🧪 Testing

### Manual Trigger (Development)

Jobs can be manually triggered via JMX or custom endpoints:

```bash
# Trigger message cleanup
curl -X POST http://localhost:8085/admin/jobs/message-cleanup

# Trigger cache cleanup
curl -X POST http://localhost:8085/admin/jobs/cache-cleanup
```

### Dry Run Mode

Enable dry run to preview deletions without actual removal:

```yaml
app:
  scheduler:
    dry-run: true
```

## 🔒 Security

- Service runs as non-root user in Docker
- Database credentials via environment variables
- No external API exposure
- Internal health checks only

## 📝 Logs

All cleanup operations are logged:

```
2026-02-13 02:00:00 [scheduler-1] INFO  MessageCleanupJob - Starting cleanup of undelivered messages...
2026-02-13 02:00:05 [scheduler-1] INFO  MessageCleanupJob - Cleaned up 1234 undelivered messages older than 1 year
```

Logs stored in: `logs/scheduled-jobs.log`

## 🗓️ Best Practices

1. **Monitor logs** for cleanup counts
2. **Alert on failures** via metrics
3. **Backup data** before major policy changes
4. **Test schedules** in staging first
5. **Document custom policies** for users

## 📚 References

- [Spring Scheduling](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling)
- [Cron Expressions](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html)

## 📄 License

MIT License
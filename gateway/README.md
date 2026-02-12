# WhatsApp Clone - API Gateway Service

## 📋 Mô tả

API Gateway là điểm truy cập duy nhất (single entry point) cho tất cả các client requests trong hệ thống WhatsApp Clone. Service này xử lý routing, authentication, rate limiting, và load balancing.

## 🎯 Tính năng chính

### Core Features
- ✅ **Routing Management**: Định tuyến requests đến các microservices
- ✅ **JWT Authentication**: Xác thực người dùng dựa trên JWT tokens
- ✅ **Rate Limiting**: Giới hạn số lượng requests per user/IP
- ✅ **WebSocket Support**: Hỗ trợ real-time chat qua WebSocket
- ✅ **Circuit Breaker**: Xử lý lỗi và failover tự động
- ✅ **Request/Response Logging**: Ghi log chi tiết cho debugging
- ✅ **CORS Configuration**: Cấu hình CORS cho web/mobile clients

### Security Features
- 🔒 JWT-based authentication
- 🔒 Token validation và refresh
- 🔒 Role-based access control (RBAC)
- 🔒 Rate limiting per user tier

### Monitoring & Observability
- 📊 Prometheus metrics
- 📊 Health checks
- 📊 Distributed tracing
- 📊 Request logging

## 🏗️ Kiến trúc

```
Client (Web/Mobile)
        ↓
    API Gateway (Port 8080)
        ↓
    ├── User Service (Port 8081)
    ├── Chat Service (Port 8082)
    └── WebSocket → Chat Service
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker Desktop
- Git

### 1. Build Project

```bash
# Build tất cả modules
mvn clean install

# Build chỉ API Gateway
mvn clean install -pl api-gateway -am

# Build và skip tests
mvn clean install -DskipTests
```

### 2. Run Locally (Without Docker)

```bash
# Chạy từ root directory
mvn spring-boot:run -pl api-gateway

# Hoặc chạy JAR file
java -jar api-gateway/target/api-gateway.jar
```

Gateway sẽ chạy tại: http://localhost:8080

### 3. Run with Docker

#### Build Docker Image

```bash
# Sử dụng Maven Jib plugin (khuyến nghị)
mvn clean compile jib:dockerBuild -pl api-gateway

# Hoặc sử dụng Dockerfile
docker build -t whatsapp-clone/api-gateway:latest -f api-gateway/Dockerfile .
```

#### Run Docker Container

```bash
# Run single container
docker run -d \
  --name api-gateway \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e REDIS_HOST=redis \
  -e JWT_SECRET=your-secret-key \
  whatsapp-clone/api-gateway:latest

# Check logs
docker logs -f api-gateway
```

### 4. Run Full Stack with Docker Compose

```bash
# Start all services
docker-compose up -d

# Start with monitoring (Prometheus + Grafana)
docker-compose --profile monitoring up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f api-gateway

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## 📁 Cấu trúc Project

```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/whatsapp/gateway/
│   │   │   ├── config/                    # Configuration classes
│   │   │   │   ├── SecurityConfig.java    # Spring Security configuration
│   │   │   │   ├── WebSocketConfig.java   # WebSocket configuration
│   │   │   │   └── RateLimitConfig.java   # Rate limiting configuration
│   │   │   ├── filter/                    # Custom filters
│   │   │   │   ├── AuthenticationFilter.java  # JWT authentication
│   │   │   │   └── LoggingFilter.java         # Request/response logging
│   │   │   └── GatewayApplication.java    # Main application class
│   │   └── resources/
│   │       ├── application.yml            # Main configuration
│   │       └── application-prod.yml       # Production configuration
│   └── test/                              # Test classes
├── Dockerfile                             # Docker image definition
└── pom.xml                                # Maven dependencies
```

## ⚙️ Configuration

### Application Properties

#### Development (application.yml)
```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8081
          predicates:
            - Path=/api/users/**,/api/auth/**

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 hours
```

#### Production (application-prod.yml)
```yaml
logging:
  level:
    root: WARN
    com.whatsapp.gateway: INFO

resilience4j:
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |
| `SERVER_PORT` | Server port | `8080` |
| `JWT_SECRET` | JWT signing secret | Required |
| `REDIS_HOST` | Redis hostname | `redis` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password | Empty |
| `USER_SERVICE_URL` | User service URL | `http://user-service:8081` |
| `CHAT_SERVICE_URL` | Chat service URL | `http://chat-service:8082` |

## 🔐 Authentication

### JWT Token Format

```json
{
  "sub": "user-id-123",
  "username": "john.doe",
  "email": "john@example.com",
  "roles": ["USER", "PREMIUM"],
  "iat": 1703001234,
  "exp": 1703087634
}
```

### Public Endpoints (No Authentication)

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /actuator/health`
- `GET /actuator/info`

### Protected Endpoints (Authentication Required)

- All other `/api/**` endpoints
- WebSocket connections `/ws/**`

## 🚦 Rate Limiting

### User Tiers

| Tier | Requests/Minute | Description |
|------|-----------------|-------------|
| Anonymous | 10 | Unauthenticated users |
| Authenticated | 100 | Standard logged-in users |
| Premium | 500 | Paid/premium users |
| WebSocket | 50 messages/min | Real-time messaging |

## 🔍 Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "redis": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Metrics (Prometheus)

```bash
curl http://localhost:8080/actuator/prometheus
```

### Grafana Dashboards

Access Grafana at: http://localhost:3000
- Username: `admin`
- Password: `admin123`

## 🧪 Testing

### Run Unit Tests

```bash
mvn test -pl api-gateway
```

### Run Integration Tests

```bash
mvn verify -pl api-gateway
```

### Test API Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Login (get JWT token)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass"}'

# Call protected endpoint
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Connection Refused to Redis
```bash
# Check Redis is running
docker ps | grep redis

# Check Redis connection
docker exec -it whatsapp-redis redis-cli ping
```

#### 2. JWT Token Invalid
- Check `JWT_SECRET` environment variable
- Ensure token hasn't expired
- Verify token format: `Bearer <token>`

#### 3. Service Unavailable (503)
- Check downstream services are running
- Check circuit breaker status
- Review logs: `docker logs -f api-gateway`

### Debug Mode

```bash
# Enable debug logging
export LOGGING_LEVEL_COM_WHATSAPP_GATEWAY=DEBUG

# Run with debug
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.whatsapp.gateway=DEBUG"
```

## 📊 Performance Tuning

### JVM Options

```bash
JAVA_OPTS="-Xms512m -Xmx1024m \
           -XX:+UseG1GC \
           -XX:MaxGCPauseMillis=200"
```

### Connection Pool Settings

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 500
          max-idle-time: 30s
```

## 🔄 CI/CD

### Build Commands

```bash
# Development build
mvn clean install -Pdev

# Docker build
mvn clean install -Pdocker

# Production build
mvn clean install -Pprod -DskipTests
```

## 📚 API Documentation

API documentation available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open Pull Request

## 📝 License

This project is licensed under the MIT License.

## 👥 Team

WhatsApp Clone Development Team

## 📞 Support

For issues and questions:
- Create GitHub Issue
- Email: support@whatsapp-clone.com

# WhatsApp Clone – Deployment Guide

> Complete guide for deploying the WhatsApp Clone system from development to production.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Repository Structure](#2-repository-structure)
3. [Environment Configuration](#3-environment-configuration)
4. [Local Development Deployment](#4-local-development-deployment)
5. [Docker Compose Deployment (Recommended)](#5-docker-compose-deployment-recommended)
6. [Building Individual Services](#6-building-individual-services)
7. [Database Initialisation](#7-database-initialisation)
8. [Service Configuration Reference](#8-service-configuration-reference)
9. [Health Verification](#9-health-verification)
10. [Production Hardening Checklist](#10-production-hardening-checklist)
11. [CI/CD Pipeline](#11-cicd-pipeline)
12. [Rollback Procedures](#12-rollback-procedures)
13. [Scaling](#13-scaling)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Prerequisites

### Required Software

| Tool | Minimum Version | Install |
|------|----------------|---------|
| **Docker** | 20.10 | https://docs.docker.com/get-docker/ |
| **Docker Compose** | 2.0 (v2 CLI) | Bundled with Docker Desktop |
| **Java JDK** | 17 | https://adoptium.net (for local dev only) |
| **Maven** | 3.9 | https://maven.apache.org (for local dev only) |
| **Git** | 2.x | https://git-scm.com |

### System Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| RAM | 8 GB | 16 GB |
| Disk | 10 GB free | 20 GB free |
| CPU | 4 cores | 8 cores |
| OS | Linux / macOS / Windows (WSL2) | Linux |

### Verify Installation

```bash
docker --version          # Docker version 24.x.x
docker compose version    # Docker Compose version v2.x.x
java -version             # openjdk 17.x.x (local dev only)
mvn -version              # Apache Maven 3.9.x (local dev only)
```

---

## 2. Repository Structure

```
whatsapp/
├── pom.xml                        ← Parent POM (multi-module Maven)
├── common-lib/                    ← Shared DTOs, exceptions, utilities
├── gateway/                       ← API Gateway  (port 8080)
├── user/                          ← User Service (port 8081)
├── chat/                          ← Chat Service (port 8082)
├── notification/                  ← Notification Service (port 8084)
├── message-processor/             ← Message Processor (port 8085)
├── scheduled-jobs/                ← Scheduled Jobs (port 8086)
├── infrastructure/
│   ├── docker/
│   │   ├── docker-compose.yml     ← Main compose file (16 services)
│   │   ├── docker-compose.override.yml  ← Dev overrides
│   │   ├── docker-compose.prod.yml      ← Production overrides
│   │   └── nginx/nginx.conf
│   ├── prometheus/prometheus.yml
│   └── elk/logstash.conf
├── scripts/
│   ├── deploy.sh          ← Full deployment script
│   ├── start.sh           ← Start all containers
│   ├── stop.sh            ← Stop all containers
│   ├── clean.sh           ← Remove containers (optionally volumes)
│   ├── logs.sh            ← View service logs
│   ├── health-check.sh    ← Verify all services are healthy
│   └── db-migrate.sh      ← Run database migrations
└── docs/
    ├── api/
    ├── architecture/
    └── deployment/
```

---

## 3. Environment Configuration

### 3.1 Create `.env` File

Copy the example and fill in your values:

```bash
cp .env.example .env
```

**`.env` reference:**

```dotenv
# ── PostgreSQL ──────────────────────────────────────────────────────────────
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=whatsapp_db
POSTGRES_USER=whatsapp
POSTGRES_PASSWORD=whatsapp123          # CHANGE IN PRODUCTION

# ── MongoDB ─────────────────────────────────────────────────────────────────
MONGO_HOST=mongodb
MONGO_PORT=27017
MONGO_DB=whatsapp_chat
MONGO_USER=whatsapp
MONGO_PASSWORD=whatsapp123             # CHANGE IN PRODUCTION

# ── Redis ───────────────────────────────────────────────────────────────────
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=whatsapp123             # CHANGE IN PRODUCTION

# ── RabbitMQ ────────────────────────────────────────────────────────────────
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=whatsapp
RABBITMQ_PASSWORD=whatsapp123          # CHANGE IN PRODUCTION

# ── JWT ─────────────────────────────────────────────────────────────────────
JWT_SECRET=your-256-bit-secret-change-this-in-production   # MIN 32 CHARS
JWT_EXPIRATION=86400000                # 24 hours in ms

# ── Firebase (optional – for push notifications) ─────────────────────────────
FIREBASE_CREDENTIALS_PATH=/app/config/firebase-credentials.json
FIREBASE_DATABASE_URL=https://your-project.firebaseio.com

# ── Spring Profiles ──────────────────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=docker
```

### 3.2 Firebase Credentials (Optional)

To enable FCM push notifications:

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Project Settings → Service Accounts → Generate new private key
3. Save as `firebase-credentials.json`
4. Mount into containers via the volume in `docker-compose.yml`:
   ```yaml
   volumes:
     - ./firebase-credentials.json:/app/config/firebase-credentials.json:ro
   ```

Without Firebase credentials the notification service starts but skips FCM calls (logs a warning).

---

## 4. Local Development Deployment

### 4.1 Start Infrastructure Only

Start only databases, Redis, and RabbitMQ – run services locally in your IDE:

```bash
# Start infrastructure services
docker compose -f infrastructure/docker/docker-compose.yml \
  up -d postgres mongodb redis rabbitmq

# Verify infrastructure
docker compose -f infrastructure/docker/docker-compose.yml ps
```

### 4.2 Build and Run Services (Maven)

```bash
# Build all modules (skip tests for speed)
mvn clean install -DskipTests

# Run a specific service
mvn spring-boot:run -pl gateway       # API Gateway  :8080
mvn spring-boot:run -pl user          # User Service :8081
mvn spring-boot:run -pl chat          # Chat Service :8082
mvn spring-boot:run -pl notification  # Notif Svc    :8084
mvn spring-boot:run -pl message-processor  # MsgProc :8085
mvn spring-boot:run -pl scheduled-jobs     # Jobs    :8086
```

### 4.3 IDE Configuration

Set the following environment variables in your run configuration:

```
SPRING_PROFILES_ACTIVE=local
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/whatsapp_db
SPRING_DATASOURCE_USERNAME=whatsapp
SPRING_DATASOURCE_PASSWORD=whatsapp123
SPRING_DATA_MONGODB_HOST=localhost
SPRING_DATA_REDIS_HOST=localhost
SPRING_RABBITMQ_HOST=localhost
JWT_SECRET=local-dev-secret-at-least-32-chars
```

---

## 5. Docker Compose Deployment (Recommended)

### 5.1 One-Command Full Deployment

```bash
# Linux / macOS
chmod +x scripts/*.sh
./scripts/deploy.sh

# Windows (PowerShell)
docker compose -f infrastructure/docker/docker-compose.yml up -d --build
```

The `deploy.sh` script:
1. Checks prerequisites (Docker, Docker Compose)
2. Builds all 6 microservice Docker images (~5–10 min first time, cached afterwards)
3. Starts infrastructure (PostgreSQL, MongoDB, Redis, RabbitMQ) and waits for health
4. Starts all microservices
5. Starts monitoring stack (Prometheus, Grafana)
6. Starts logging stack (Elasticsearch, Logstash, Kibana)
7. Starts Nginx reverse proxy
8. Runs health checks on all 16 services
9. Prints service URLs and credentials

**Expected total time**: ~10 minutes first run, ~3 minutes on subsequent runs.

### 5.2 Manual Step-by-Step

```bash
# Step 1 – Build images
docker compose -f infrastructure/docker/docker-compose.yml build

# Step 2 – Start infrastructure
docker compose -f infrastructure/docker/docker-compose.yml \
  up -d postgres mongodb redis rabbitmq

# Step 3 – Wait for infrastructure to be healthy (≈30 s)
docker compose -f infrastructure/docker/docker-compose.yml ps

# Step 4 – Start microservices
docker compose -f infrastructure/docker/docker-compose.yml \
  up -d gateway user-service chat-service notification-service \
       message-processor scheduled-jobs

# Step 5 – Start monitoring
docker compose -f infrastructure/docker/docker-compose.yml \
  up -d prometheus grafana elasticsearch logstash kibana

# Step 6 – Start reverse proxy
docker compose -f infrastructure/docker/docker-compose.yml up -d nginx
```

### 5.3 Service Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **API Gateway** | http://localhost:8080 | – |
| **Web (Nginx)** | http://localhost | – |
| **User Service** | http://localhost:8081 | – |
| **Chat Service** | http://localhost:8082 | – |
| **Notification** | http://localhost:8084 | – |
| **Message Processor** | http://localhost:8085 | – |
| **Scheduled Jobs** | http://localhost:8086 | – |
| **RabbitMQ Management** | http://localhost:15672 | `whatsapp` / `whatsapp123` |
| **Prometheus** | http://localhost:9090 | – |
| **Grafana** | http://localhost:3000 | `admin` / `admin123` |
| **Kibana** | http://localhost:5601 | – |

### 5.4 Common Operations

```bash
# View all container statuses
docker compose -f infrastructure/docker/docker-compose.yml ps

# Tail logs for a service
./scripts/logs.sh gateway
./scripts/logs.sh chat-service

# Restart a single service
docker compose -f infrastructure/docker/docker-compose.yml restart gateway

# Stop everything (keep data volumes)
./scripts/stop.sh

# Stop and remove containers (keep volumes)
./scripts/clean.sh

# Full teardown including data volumes ⚠️
./scripts/clean.sh --volumes
```

---

## 6. Building Individual Services

### Dockerfile Overview

All services use **multi-stage builds**:

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn clean package -DskipTests -pl <service> --also-make

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=builder /app/<service>/target/*.jar app.jar
EXPOSE <port>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build a Single Service

```bash
# From the workspace root
docker build -t whatsapp-clone/gateway:latest \
  -f gateway/Dockerfile .

docker build -t whatsapp-clone/user-service:latest \
  -f user/Dockerfile .

docker build -t whatsapp-clone/chat-service:latest \
  -f chat/Dockerfile .

docker build -t whatsapp-clone/notification:latest \
  -f notification/Dockerfile .

docker build -t whatsapp-clone/message-processor:latest \
  -f message-processor/Dockerfile .

docker build -t whatsapp-clone/scheduled-jobs:latest \
  -f scheduled-jobs/Dockerfile .
```

### Rebuild and Redeploy One Service

```bash
docker compose -f infrastructure/docker/docker-compose.yml \
  build --no-cache gateway

docker compose -f infrastructure/docker/docker-compose.yml \
  up -d gateway
```

---

## 7. Database Initialisation

### 7.1 PostgreSQL – Flyway Migrations

Migrations run automatically on application startup (Flyway auto-migrate is enabled).

Migration files location: `user/src/main/resources/db/migration/`

```
V1__create_users_table.sql
V2__create_conversations_tables.sql
V3__add_indexes.sql
...
```

Manual migration run:

```bash
# Run all pending migrations
./scripts/db-migrate.sh postgres

# Or via Maven Flyway plugin
mvn flyway:migrate -pl user \
  -Dflyway.url=jdbc:postgresql://localhost:5432/whatsapp_db \
  -Dflyway.user=whatsapp \
  -Dflyway.password=whatsapp123
```

### 7.2 MongoDB – Index Initialisation

MongoDB indexes are created programmatically via `@Document` and `@Indexed` annotations in Spring Data, or via the init script at startup.

Manual index creation (connect to MongoDB shell):

```bash
docker exec -it whatsapp-mongodb \
  mongosh -u whatsapp -p whatsapp123 --authenticationDatabase admin whatsapp_chat

# Inside mongosh:
db.messages.createIndex({ conversationId: 1, createdAt: -1 })
db.messages.createIndex({ senderId: 1, createdAt: -1 })
db.messages.createIndex({ sentAt: 1 }, { expireAfterSeconds: 7776000 })
```

### 7.3 Database Access

```bash
# PostgreSQL
docker exec -it whatsapp-postgres \
  psql -U whatsapp -d whatsapp_db

# MongoDB
docker exec -it whatsapp-mongodb \
  mongosh -u whatsapp -p whatsapp123 --authenticationDatabase admin

# Redis CLI
docker exec -it whatsapp-redis redis-cli -a whatsapp123
```

---

## 8. Service Configuration Reference

### Port Map

| Service | Container Name | Internal Port | Host Port |
|---------|----------------|--------------|-----------|
| Nginx | whatsapp-nginx | 80, 443 | 80, 443 |
| API Gateway | whatsapp-gateway | 8080 | 8080 |
| User Service | whatsapp-user-service | 8081 | 8081 |
| Chat Service | whatsapp-chat-service | 8082 | 8082 |
| Notification | whatsapp-notification-service | 8084 | 8084 |
| Message Processor | whatsapp-message-processor | 8085 | 8085 |
| Scheduled Jobs | whatsapp-scheduled-jobs | 8086 | 8086 |
| PostgreSQL | whatsapp-postgres | 5432 | 5432 |
| MongoDB | whatsapp-mongodb | 27017 | 27017 |
| Redis | whatsapp-redis | 6379 | 6379 |
| RabbitMQ | whatsapp-rabbitmq | 5672, 15672 | 5672, 15672 |
| Prometheus | whatsapp-prometheus | 9090 | 9090 |
| Grafana | whatsapp-grafana | 3000 | 3000 |
| Elasticsearch | whatsapp-elasticsearch | 9200 | 9200 |
| Logstash | whatsapp-logstash | 5000 | 5000 |
| Kibana | whatsapp-kibana | 5601 | 5601 |

### JVM Memory Settings

Adjust `JAVA_OPTS` per service in the respective `Dockerfile`:

| Service | Default Heap | Recommended Max (prod) |
|---------|-------------|----------------------|
| Gateway | 256–512 MB | 512 MB |
| User Service | 256–512 MB | 512 MB |
| Chat Service | 512 MB–1 GB | 1 GB (WebSocket overhead) |
| Notification | 256–512 MB | 512 MB |
| Message Processor | 256–512 MB | 512 MB |
| Scheduled Jobs | 256–512 MB | 256 MB |

```dockerfile
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"
```

---

## 9. Health Verification

### Automated Check

```bash
./scripts/health-check.sh
```

### Manual Checks

```bash
# Gateway
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# All services in one loop
for port in 8080 8081 8082 8085 8086; do
  echo "=== Port $port ===" && \
  curl -s http://localhost:$port/actuator/health
done

# Notification (different base path)
curl -s http://localhost:8084/api/v1/actuator/health

# Check all Docker containers are running
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Expected Health Response

```json
{
  "status": "UP",
  "components": {
    "db":        { "status": "UP" },
    "redis":     { "status": "UP" },
    "rabbit":    { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Verify API is Working

```bash
# Register a test user
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_deploy",
    "email": "test@deploy.com",
    "phoneNumber": "+84900000001",
    "password": "TestPass@123",
    "displayName": "Deploy Test"
  }' | python3 -m json.tool
```

Expected: HTTP 201 with user JSON.

---

## 10. Production Hardening Checklist

### Security

- [ ] Change **all default passwords** in `.env`
- [ ] Generate a strong `JWT_SECRET` (min 32 random characters):
  ```bash
  openssl rand -base64 48
  ```
- [ ] Enable **HTTPS** in `nginx.conf` (TLS certificate via Let's Encrypt or ACM)
- [ ] Remove or restrict direct host port bindings for databases (`5432`, `27017`, `6379`)
- [ ] Enable **authentication** on Prometheus and Kibana
- [ ] Set `spring.security.oauth2.resourceserver.jwt.issuer-uri` in production
- [ ] Enable `xpack.security.enabled=true` in Elasticsearch

### Performance

- [ ] Set `SPRING_PROFILES_ACTIVE=prod` (disables debug logs)
- [ ] Tune HikariCP pool sizes per load (`spring.datasource.hikari.maximum-pool-size`)
- [ ] Configure MongoDB connection pool (`spring.data.mongodb.uri` with pool params)
- [ ] Set Redis `maxmemory-policy` to `allkeys-lru`
- [ ] Enable PostgreSQL connection pooling via PgBouncer

### Reliability

- [ ] Configure Docker `restart: always` (already set in `docker-compose.yml`)
- [ ] Set up **off-site database backups** (see backup section below)
- [ ] Configure **RabbitMQ durable queues** (already enabled)
- [ ] Set Elasticsearch retention policy (ILM – Index Lifecycle Management)
- [ ] Configure Grafana alert rules for key metrics (error rate, p99 latency)

### Backups

```bash
# PostgreSQL backup
docker exec whatsapp-postgres \
  pg_dump -U whatsapp whatsapp_db > backup_postgres_$(date +%Y%m%d).sql

# MongoDB backup
docker exec whatsapp-mongodb \
  mongodump -u whatsapp -p whatsapp123 \
  --authenticationDatabase admin \
  --archive > backup_mongo_$(date +%Y%m%d).archive

# Restore PostgreSQL
docker exec -i whatsapp-postgres \
  psql -U whatsapp whatsapp_db < backup_postgres_20260224.sql

# Restore MongoDB
docker exec -i whatsapp-mongodb \
  mongorestore -u whatsapp -p whatsapp123 \
  --authenticationDatabase admin \
  --archive < backup_mongo_20260224.archive
```

---

## 11. CI/CD Pipeline

### GitHub Actions – Build & Test

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Build and test
        run: mvn clean verify -B

      - name: Build Docker images
        run: |
          docker build -f gateway/Dockerfile -t whatsapp-gateway:${{ github.sha }} .
          docker build -f user/Dockerfile    -t whatsapp-user:${{ github.sha }}    .
          docker build -f chat/Dockerfile    -t whatsapp-chat:${{ github.sha }}    .

      - name: Run integration tests
        run: |
          docker compose -f infrastructure/docker/docker-compose.yml \
            up -d postgres mongodb redis rabbitmq
          sleep 30
          mvn verify -Pintegration-tests
```

### GitHub Actions – Deploy to Production

```yaml
name: Deploy

on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production

    steps:
      - uses: actions/checkout@v4

      - name: Log in to container registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push images
        run: |
          VERSION=${{ github.ref_name }}
          for svc in gateway user chat notification message-processor scheduled-jobs; do
            docker build -f $svc/Dockerfile \
              -t ghcr.io/${{ github.repository }}/$svc:$VERSION \
              -t ghcr.io/${{ github.repository }}/$svc:latest .
            docker push ghcr.io/${{ github.repository }}/$svc:$VERSION
            docker push ghcr.io/${{ github.repository }}/$svc:latest
          done

      - name: Deploy to server
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd /opt/whatsapp-clone
            git pull
            docker compose -f infrastructure/docker/docker-compose.prod.yml pull
            docker compose -f infrastructure/docker/docker-compose.prod.yml up -d
            ./scripts/health-check.sh
```

---

## 12. Rollback Procedures

### Quick Rollback (Docker)

```bash
# Revert to previous image tag
docker compose -f infrastructure/docker/docker-compose.yml \
  stop gateway

docker compose -f infrastructure/docker/docker-compose.yml \
  run --rm -e IMAGE_TAG=v1.2.3 gateway

# Or pull a specific version
docker pull ghcr.io/yourorg/whatsapp-gateway:v1.2.3
docker compose -f infrastructure/docker/docker-compose.yml up -d gateway
```

### Database Rollback

```bash
# Flyway undo (requires Flyway Teams for undo migrations)
mvn flyway:undo -pl user \
  -Dflyway.url=jdbc:postgresql://localhost:5432/whatsapp_db \
  -Dflyway.user=whatsapp \
  -Dflyway.password=whatsapp123

# Restore from backup (last resort)
docker exec -i whatsapp-postgres \
  psql -U whatsapp whatsapp_db < backup_postgres_YYYYMMDD.sql
```

### Blue-Green Deployment

For zero-downtime deployments:

1. Bring up new version on alternate ports (`8090`, `8091`, etc.)
2. Verify health on new containers
3. Update Nginx upstream to point to new containers
4. Drain and stop old containers

```nginx
upstream user_service {
    server user-service-blue:8081;   # active
    # server user-service-green:8091;  # standby
}
```

---

## 13. Scaling

### Scale Stateless Services

```bash
# Scale User Service to 3 replicas
docker compose -f infrastructure/docker/docker-compose.yml \
  up -d --scale user-service=3

# Scale Message Processor to 5 replicas
docker compose -f infrastructure/docker/docker-compose.yml \
  up -d --scale message-processor=5
```

### Scale Chat Service (WebSocket)

Chat Service is stateful (WebSocket connections). Before scaling:

1. Enable Redis Pub/Sub in `chat/src/main/resources/application.yaml`
2. Enable sticky sessions in `nginx.conf`:
   ```nginx
   upstream chat_service {
       ip_hash;  # sticky sessions
       server chat-service:8082;
   }
   ```
3. Scale:
   ```bash
   docker compose -f infrastructure/docker/docker-compose.yml \
     up -d --scale chat-service=3
   ```

### Scale Databases

**PostgreSQL read replicas** (manual):
```yaml
# docker-compose.prod.yml addition
postgres-replica:
  image: postgres:15-alpine
  environment:
    POSTGRES_MASTER_HOST: postgres
  command: |
    postgres -c recovery.conf
```

**MongoDB replica set**:
```bash
docker exec -it whatsapp-mongodb mongosh
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "mongodb-primary:27017" },
    { _id: 1, host: "mongodb-secondary1:27017" },
    { _id: 2, host: "mongodb-secondary2:27017" }
  ]
})
```

---

## 14. Troubleshooting

### Service Won't Start

```bash
# Check container logs
docker logs whatsapp-gateway --tail=100

# Check exit code
docker inspect whatsapp-gateway | grep -A5 '"State"'

# Check all stopped containers
docker ps -a --filter "status=exited"
```

**Common causes:**
- Database not healthy yet → increase `start_period` in health check
- Port already in use → `lsof -i :8080` / `netstat -ano | findstr 8080`
- Missing environment variable → check docker-compose.yml `environment` section

### Database Connection Refused

```bash
# Verify database is accepting connections
docker exec whatsapp-postgres pg_isready -U whatsapp

# Test connection from service container
docker exec whatsapp-user-service \
  nc -zv postgres 5432

# Check PostgreSQL logs
docker logs whatsapp-postgres --tail=50
```

### Out of Memory (OOMKilled)

```bash
# Check which container was OOMKilled
docker inspect whatsapp-chat-service | grep OOMKilled

# Check current memory usage
docker stats --no-stream

# Increase Docker Desktop memory:
# Docker Desktop → Settings → Resources → Memory → raise to 12–16 GB

# Or increase JVM heap in Dockerfile:
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
```

### Messages Not Delivered

1. Check RabbitMQ management: http://localhost:15672
   - Verify queues exist: `message.sent`, `message.delivered`
   - Check queue depth (should be ~0 if processor is running)
2. Check Message Processor logs:
   ```bash
   docker logs whatsapp-message-processor --tail=100 | grep ERROR
   ```
3. Check Redis inbox cache:
   ```bash
   docker exec whatsapp-redis redis-cli -a whatsapp123 \
     KEYS "inbox:*"
   ```

### WebSocket Connection Fails

1. Verify Chat Service is running: `curl http://localhost:8082/actuator/health`
2. Check Nginx WebSocket proxy config in `nginx.conf`:
   ```nginx
   proxy_http_version 1.1;
   proxy_set_header Upgrade $http_upgrade;
   proxy_set_header Connection "upgrade";
   ```
3. Confirm JWT token is valid (not expired) when connecting

### Push Notifications Not Received

1. Verify `FIREBASE_CREDENTIALS_PATH` is set and file exists
2. Check Notification Service logs:
   ```bash
   docker logs whatsapp-notification-service | grep -i "fcm\|firebase\|error"
   ```
3. Test token registration:
   ```bash
   curl -X POST http://localhost:8084/notifications/register \
     -H "Content-Type: application/json" \
     -d '{"userId":"test","token":"test-token","platform":"ANDROID"}'
   ```

### Disk Space Issues

```bash
# Check Docker disk usage
docker system df

# Clean dangling images and stopped containers
docker system prune -f

# Clean build cache
docker builder prune -f

# Remove unused volumes (⚠️ data loss risk)
docker volume prune -f
```

### Complete Reset

```bash
# Stop all containers
docker compose -f infrastructure/docker/docker-compose.yml down

# Remove all project containers, networks, volumes
docker compose -f infrastructure/docker/docker-compose.yml down -v --remove-orphans

# Clean Docker build cache
docker builder prune -a -f

# Redeploy from scratch
./scripts/deploy.sh
```

---

*Last updated: February 2026 | Version: 1.0.0*

For Docker Desktop-specific guidance, see [Docker Desktop Guide](docker-desktop-guide.md).  
For infrastructure architecture details, see [Infrastructure Setup Summary](infrastructure-setup-summary.md).


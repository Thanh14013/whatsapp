# Getting Started with WhatsApp Clone

Welcome! This guide will help you get the WhatsApp Clone up and running on your machine in **under 15 minutes**.

## 🎯 What You'll Get

A complete, production-ready WhatsApp Clone with:

- ✅ Real-time messaging (WebSocket)
- ✅ User authentication
- ✅ Push notifications
- ✅ Message delivery tracking
- ✅ Monitoring dashboards
- ✅ Centralized logging

**Total: 16 Docker containers running locally**

## ⚡ Quick Setup (3 Steps)

### Step 1: Install Docker Desktop

**Download and install Docker Desktop:**

- Windows/Mac: https://www.docker.com/products/docker-desktop
- Minimum 8GB RAM (16GB recommended)

**Verify installation:**

```bash
docker --version
docker-compose --version
```

### Step 2: Clone and Deploy

```bash
# Clone repository
git clone <repository-url>
cd whatsapp

# Deploy everything (one command!)
# For Linux/Mac:
chmod +x scripts/*.sh
./scripts/deploy.sh

# For Windows (PowerShell or Git Bash):
docker-compose -f infrastructure/docker/docker-compose.yml up -d --build
```

**What happens:**

1. Builds 6 microservice images (5-10 min first time)
2. Starts databases (PostgreSQL, MongoDB, Redis)
3. Starts message queue (RabbitMQ)
4. Starts all microservices
5. Sets up monitoring (Prometheus, Grafana)
6. Sets up logging (ELK Stack)
7. Configures reverse proxy (Nginx)

☕ **Grab a coffee!** First-time setup takes ~10 minutes.

### Step 3: Verify Everything Works

```bash
# Check health of all services
./scripts/health-check.sh

# Or manually check
docker ps

# You should see 16 running containers
```

## 🎉 You're Ready!

### Access the Application

**Main Endpoints:**

- **API Gateway**: http://localhost:8080
  - Try: http://localhost:8080/actuator/health
- **Web Access**: http://localhost (via Nginx)

**Admin Dashboards:**

- **RabbitMQ**: http://localhost:15672
  - Username: `whatsapp`
  - Password: `whatsapp123`

- **Grafana** (Metrics): http://localhost:3000
  - Username: `admin`
  - Password: `admin123`

- **Kibana** (Logs): http://localhost:5601

- **Prometheus**: http://localhost:9090

## 📱 Test the Application

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "phoneNumber": "+1234567890",
    "password": "SecurePass123!",
    "displayName": "John Doe"
  }'
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePass123!"
  }'
```

Copy the JWT token from the response.

### 3. Get User Profile

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. Connect to WebSocket (for real-time chat)

Use a WebSocket client like [websocat](https://github.com/vi/websocat) or Postman:

```bash
ws://localhost:8082/ws/chat?userId=YOUR_USER_ID
```

### 5. Send a Message

```json
{
  "action": "send_message",
  "receiverId": "another-user-id",
  "content": "Hello from WhatsApp Clone!",
  "contentType": "TEXT"
}
```

## 🔧 Common Operations

### View Logs

```bash
# All services
./scripts/logs.sh

# Specific service
./scripts/logs.sh gateway
./scripts/logs.sh chat-service
```

### Stop Everything

```bash
./scripts/stop.sh
```

### Start Again

```bash
./scripts/start.sh
```

### Clean Up (Remove Containers)

```bash
# Keep data
./scripts/clean.sh

# Remove everything (including databases)
./scripts/clean.sh --volumes
```

### Access Databases Directly

```bash
# PostgreSQL
docker exec -it whatsapp-postgres psql -U whatsapp -d whatsapp_db

# MongoDB
docker exec -it whatsapp-mongodb mongosh -u whatsapp -p whatsapp123

# Redis
docker exec -it whatsapp-redis redis-cli
```

## 📊 Monitoring & Observability

### Grafana Dashboards

1. Go to http://localhost:3000 (admin/admin123)
2. Add Prometheus data source:
   - URL: `http://prometheus:9090`
3. Import dashboards:
   - Spring Boot 2.1 Statistics (ID: 10280)
   - JVM Micrometer (ID: 4701)

### Kibana Log Analysis

1. Go to http://localhost:5601
2. Create index pattern: `whatsapp-*`
3. View logs from all services
4. Filter by service: `service: "gateway"`
5. Search errors: `log_level: "ERROR"`

## 🐛 Troubleshooting

### Services Won't Start?

**Check logs:**

```bash
./scripts/logs.sh [service-name]
```

**Check resource usage:**

```bash
docker stats
```

**Increase Docker Desktop Resources:**

- Docker Desktop → Settings → Resources
- Memory: At least 8GB (16GB recommended)
- CPU: At least 4 cores

### Port Already in Use?

**Find what's using the port:**

```bash
# Linux/Mac
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

**Change ports in docker-compose.yml:**

```yaml
ports:
  - "8081:8080" # Use 8081 instead
```

### Database Connection Issues?

**Verify database is running:**

```bash
docker exec whatsapp-postgres pg_isready -U whatsapp
docker exec whatsapp-mongodb mongosh --eval "db.adminCommand('ping')"
docker exec whatsapp-redis redis-cli ping
```

### Complete Reset (Nuclear Option)

```bash
# Stop everything
./scripts/stop.sh

# Remove all containers, volumes, networks
docker-compose -f infrastructure/docker/docker-compose.yml down -v

# Clean Docker cache
docker system prune -a --volumes

# Start fresh
./scripts/deploy.sh
```

## 📚 Learn More

### System Architecture

- [System Design Document](docs/architecture/system-design.md)
- [Infrastructure Setup Summary](docs/deployment/infrastructure-setup-summary.md)

### Detailed Guides

- [Docker Desktop Deployment Guide](docs/deployment/docker-desktop-guide.md) - Complete reference
- [API Documentation](docs/api/openapi.yaml)
- [Postman Collection](docs/api/postman-collection.json)

### Service Details

| Service           | Port | Purpose               |
| ----------------- | ---- | --------------------- |
| Gateway           | 8080 | API Gateway, routing  |
| User Service      | 8081 | User management       |
| Chat Service      | 8082 | Real-time messaging   |
| Notification      | 8084 | Push notifications    |
| Message Processor | 8085 | Background processing |
| Scheduled Jobs    | 8086 | Cleanup tasks         |

### Infrastructure Details

| Component  | Port        | Credentials           |
| ---------- | ----------- | --------------------- |
| PostgreSQL | 5432        | whatsapp/whatsapp123  |
| MongoDB    | 27017       | whatsapp/whatsapp123  |
| Redis      | 6379        | password: whatsapp123 |
| RabbitMQ   | 5672, 15672 | whatsapp/whatsapp123  |

## 🚀 Next Steps

1. **Explore the API**
   - Import Postman collection from `docs/api/`
   - Try all endpoints

2. **Set up monitoring**
   - Configure Grafana dashboards
   - Set up alerts in Prometheus

3. **Customize configuration**
   - Edit `.env` file (copy from `.env.example`)
   - Modify `docker-compose.override.yml` for local dev

4. **Scale services**

   ```bash
   docker-compose -f infrastructure/docker/docker-compose.yml up -d --scale user-service=3
   ```

5. **Add your features**
   - Modify source code
   - Rebuild specific service:
     ```bash
     docker-compose -f infrastructure/docker/docker-compose.yml build gateway
     docker-compose -f infrastructure/docker/docker-compose.yml up -d gateway
     ```

## 💡 Tips

- **First time setup is slow** (building images), but subsequent starts are fast (~30 seconds)
- **Keep Docker Desktop running** - it needs to be open
- **Check Docker Desktop dashboard** - visual view of containers
- **Use `./scripts/health-check.sh` often** - quick status check
- **View logs frequently** - understand what's happening

## 🎓 Learning Resources

**Understanding the Stack:**

- Spring Boot 3: https://spring.io/projects/spring-boot
- Docker: https://docs.docker.com
- Prometheus: https://prometheus.io/docs
- Grafana: https://grafana.com/docs

**Microservices Patterns:**

- API Gateway pattern
- Circuit Breaker (Resilience4j)
- Event-driven architecture (RabbitMQ)
- CQRS with dual database (PostgreSQL + MongoDB)

## ❓ Need Help?

1. **Check the logs:**

   ```bash
   ./scripts/logs.sh [service-name]
   ```

2. **Run health check:**

   ```bash
   ./scripts/health-check.sh
   ```

3. **Review troubleshooting:**
   - See [Troubleshooting](#-troubleshooting) section above
   - Check [Docker Desktop Guide](docs/deployment/docker-desktop-guide.md)

4. **Check Docker resources:**
   - Docker Desktop → Settings → Resources
   - Ensure enough memory/CPU allocated

## ✅ Checklist

Before asking for help, verify:

- [ ] Docker Desktop is installed and running
- [ ] At least 8GB RAM allocated to Docker
- [ ] All ports are available (8080-8086, 5432, 27017, 6379, 5672, etc.)
- [ ] You ran `./scripts/deploy.sh` or `docker-compose up -d --build`
- [ ] No errors in `docker ps` output
- [ ] Health check passes: `./scripts/health-check.sh`

---

**Happy Coding! 🚀**

If everything is running, you now have a production-ready microservices application on your machine!

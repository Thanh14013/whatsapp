# WhatsApp Clone - Docker Desktop Deployment Guide

Complete guide for running the entire WhatsApp Clone system using Docker Desktop.

## Prerequisites

### Required Software

- **Docker Desktop** (Windows/Mac) or **Docker Engine** (Linux)
  - Version: 20.10 or higher
  - Download: https://www.docker.com/products/docker-desktop
- **Docker Compose** (included with Docker Desktop)
  - Version: 2.0 or higher
- **Git** (for cloning the repository)

### System Requirements

- **RAM**: Minimum 8GB, Recommended 16GB
- **Disk Space**: At least 10GB free
- **CPU**: 4 cores or more recommended

### Verify Installation

```bash
# Check Docker version
docker --version

# Check Docker Compose version
docker-compose --version

# Verify Docker is running
docker ps
```

## Architecture Overview

### Microservices (6 services)

- **Gateway** (Port 8080): API Gateway, routing, authentication
- **User Service** (Port 8081): User management, profiles
- **Chat Service** (Port 8082): Real-time messaging, WebSocket
- **Notification Service** (Port 8084): Push notifications (FCM/APNS)
- **Message Processor** (Port 8085): Background processing, delivery tracking
- **Scheduled Jobs** (Port 8086): Cleanup, maintenance tasks

### Infrastructure (7 services)

- **PostgreSQL** (Port 5432): User and conversation metadata
- **MongoDB** (Port 27017): Message content storage
- **Redis** (Port 6379): Caching, online status
- **RabbitMQ** (Ports 5672, 15672): Message queue, event bus
- **Nginx** (Ports 80, 443): Reverse proxy, load balancing
- **Prometheus** (Port 9090): Metrics collection
- **Grafana** (Port 3000): Metrics visualization

### Logging Stack (3 services)

- **Elasticsearch** (Port 9200): Log storage
- **Logstash** (Port 5000): Log processing
- **Kibana** (Port 5601): Log visualization

**Total: 16 Docker containers**

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd whatsapp
```

### 2. Deploy Everything

```bash
# Make scripts executable (Linux/Mac)
chmod +x scripts/*.sh

# Full deployment (builds and starts all services)
./scripts/deploy.sh
```

**On Windows**, use Git Bash or WSL, or run PowerShell:

```powershell
docker-compose -f infrastructure/docker/docker-compose.yml up -d --build
```

### 3. Wait for Services to Start

The deployment script will:

1. Build all 6 microservice Docker images (~5-10 minutes first time)
2. Start infrastructure services (PostgreSQL, MongoDB, Redis, RabbitMQ)
3. Start microservices
4. Start monitoring and logging services
5. Configure Nginx

**Expected startup time: 3-5 minutes**

### 4. Verify Deployment

```bash
# Check all services are healthy
./scripts/health-check.sh

# Or check manually
docker ps
```

## Service Access

### Application URLs

- **API Gateway**: http://localhost:8080
- **Nginx Proxy**: http://localhost
- **RabbitMQ Management**: http://localhost:15672 (whatsapp/whatsapp123)

### Monitoring & Logs

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Kibana**: http://localhost:5601

### Databases (Direct Access)

```bash
# PostgreSQL
docker exec -it whatsapp-postgres psql -U whatsapp -d whatsapp_db

# MongoDB
docker exec -it whatsapp-mongodb mongosh -u whatsapp -p whatsapp123

# Redis
docker exec -it whatsapp-redis redis-cli
```

## Common Operations

### View Logs

```bash
# All services
./scripts/logs.sh

# Specific service
./scripts/logs.sh gateway
./scripts/logs.sh user-service
./scripts/logs.sh chat-service

# Or use docker-compose directly
docker-compose -f infrastructure/docker/docker-compose.yml logs -f gateway
```

### Start/Stop Services

```bash
# Start all services
./scripts/start.sh

# Stop all services
./scripts/stop.sh

# Restart specific service
docker-compose -f infrastructure/docker/docker-compose.yml restart gateway
```

### Database Migrations

```bash
# Run all migrations
./scripts/db-migrate.sh

# PostgreSQL only
./scripts/db-migrate.sh postgres

# MongoDB only
./scripts/db-migrate.sh mongodb
```

### Clean Up

```bash
# Remove containers (keep volumes/data)
./scripts/clean.sh

# Remove everything including data
./scripts/clean.sh --volumes
```

## Configuration

### Environment Variables

Located in `docker-compose.yml` for each service. Key configurations:

**Database Credentials:**

- PostgreSQL: `whatsapp/whatsapp123`
- MongoDB: `whatsapp/whatsapp123`
- Redis password: `whatsapp123`
- RabbitMQ: `whatsapp/whatsapp123`

**JWT Secret** (change for production):

```yaml
JWT_SECRET: your-256-bit-secret-key-change-this-in-production
```

### Resource Limits

Default memory limits per service:

- Gateway, User, Notification, Message-Processor, Scheduled-Jobs: 256-512MB
- Chat (WebSocket): 512-1024MB

To adjust, modify `JAVA_OPTS` in respective Dockerfiles.

### Port Mapping

To change exposed ports, edit `docker-compose.yml`:

```yaml
ports:
  - "8080:8080" # Change left side only (host:container)
```

## Development Workflow

### Code Changes

1. Modify source code
2. Rebuild specific service:

```bash
docker-compose -f infrastructure/docker/docker-compose.yml build gateway
docker-compose -f infrastructure/docker/docker-compose.yml up -d gateway
```

### Hot Reload (Optional)

For development, mount source code as volume in `docker-compose.yml`:

```yaml
volumes:
  - ./gateway/src:/app/src
```

### Debugging

```bash
# Attach to container
docker exec -it whatsapp-gateway bash

# Check Java processes
docker exec -it whatsapp-gateway ps aux | grep java

# View JVM stats
docker stats whatsapp-gateway
```

## Troubleshooting

### Service Won't Start

```bash
# Check logs
docker-compose -f infrastructure/docker/docker-compose.yml logs gateway

# Check health
docker inspect whatsapp-gateway | grep Health -A 10

# View all containers (including stopped)
docker ps -a
```

### Database Connection Issues

```bash
# Verify database is running
docker exec whatsapp-postgres pg_isready -U whatsapp

# Test connectivity from service
docker exec whatsapp-user-service ping postgres
```

### Out of Memory

```bash
# Check memory usage
docker stats

# Increase Docker Desktop memory:
# Docker Desktop → Settings → Resources → Memory
```

### Port Already in Use

```bash
# Find process using port (Linux/Mac)
lsof -i :8080

# Windows
netstat -ano | findstr :8080

# Change port in docker-compose.yml
ports:
  - "8081:8080"  # Use 8081 instead of 8080
```

### Build Failures

```bash
# Clean Docker cache
docker builder prune -a

# Rebuild without cache
docker-compose -f infrastructure/docker/docker-compose.yml build --no-cache gateway

# Check disk space
docker system df
```

### Complete Reset

```bash
# Stop everything
./scripts/stop.sh

# Remove all containers, volumes, networks
docker-compose -f infrastructure/docker/docker-compose.yml down -v

# Clean Docker system
docker system prune -a --volumes

# Redeploy
./scripts/deploy.sh
```

## Monitoring

### Prometheus Metrics

Access Prometheus at http://localhost:9090

**Useful Queries:**

```promql
# Request rate per service
rate(http_server_requests_seconds_count[5m])

# JVM memory usage
jvm_memory_used_bytes{area="heap"}

# Database connection pool
hikaricp_connections_active
```

### Grafana Dashboards

Access Grafana at http://localhost:3000 (admin/admin123)

1. Add Prometheus data source: http://prometheus:9090
2. Import dashboards:
   - Spring Boot 2.1 Statistics (ID: 10280)
   - JVM (Micrometer) (ID: 4701)

### Application Logs

Access Kibana at http://localhost:5601

1. Create index pattern: `whatsapp-*`
2. Filter by service: `service: "gateway"`
3. Search errors: `log_level: "ERROR"`

## Performance Tuning

### JVM Tuning

Edit Dockerfiles to adjust JVM parameters:

```dockerfile
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
```

### Database Optimization

**PostgreSQL:**

```bash
# Increase shared_buffers
docker exec -it whatsapp-postgres vi /var/lib/postgresql/data/postgresql.conf
```

**MongoDB:**

```bash
# Enable profiling
docker exec -it whatsapp-mongodb mongosh
use whatsapp_chat
db.setProfilingLevel(1, {slowms: 100})
```

### Redis Optimization

```bash
# Check memory usage
docker exec -it whatsapp-redis redis-cli INFO memory

# Set maxmemory policy
docker exec -it whatsapp-redis redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

## Security Best Practices

### For Production Deployment:

1. **Change default passwords** in `docker-compose.yml`
2. **Use secrets management** (Docker Secrets, Vault)
3. **Enable HTTPS** in Nginx configuration
4. **Restrict network access** using Docker networks
5. **Enable authentication** for Prometheus, Elasticsearch
6. **Use non-root images** (already implemented)
7. **Scan images for vulnerabilities**:

```bash
docker scan whatsapp-gateway
```

## Backup and Restore

### Backup Databases

```bash
# PostgreSQL
docker exec whatsapp-postgres pg_dump -U whatsapp whatsapp_db > backup_postgres.sql

# MongoDB
docker exec whatsapp-mongodb mongodump -u whatsapp -p whatsapp123 --archive > backup_mongo.archive
```

### Restore Databases

```bash
# PostgreSQL
docker exec -i whatsapp-postgres psql -U whatsapp whatsapp_db < backup_postgres.sql

# MongoDB
docker exec -i whatsapp-mongodb mongorestore -u whatsapp -p whatsapp123 --archive < backup_mongo.archive
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Deploy to Docker

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Deploy
        run: |
          ./scripts/deploy.sh --clean
```

## Additional Resources

- **API Documentation**: `/docs/api/openapi.yaml`
- **Architecture Diagram**: `/docs/architecture/system-design.md`
- **Postman Collection**: `/docs/api/postman-collection.json`

## Support

For issues or questions:

1. Check logs: `./scripts/logs.sh [service]`
2. Run health check: `./scripts/health-check.sh`
3. Review troubleshooting section above
4. Check Docker Desktop resources (memory, disk)

---

**Last Updated**: 2024
**Version**: 1.0.0

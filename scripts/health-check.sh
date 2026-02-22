#!/bin/bash

################################################################################
# WhatsApp Clone - Health Check Script
# Checks health status of all services
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_NAME="whatsapp"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Health Check - WhatsApp Clone${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# Function to check HTTP endpoint
check_http() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    
    if [ "$response" == "$expected_code" ]; then
        echo -e "${GREEN}✓${NC} $name: healthy (HTTP $response)"
        return 0
    else
        echo -e "${RED}✗${NC} $name: unhealthy (HTTP $response)"
        return 1
    fi
}

# Function to check container status
check_container() {
    local container=$1
    
    if docker ps --filter "name=$container" --filter "status=running" | grep -q "$container"; then
        echo -e "${GREEN}✓${NC} $container: running"
        return 0
    else
        echo -e "${RED}✗${NC} $container: not running"
        return 1
    fi
}

# Function to check database connectivity
check_postgres() {
    if docker exec whatsapp-postgres pg_isready -U whatsapp &>/dev/null; then
        echo -e "${GREEN}✓${NC} PostgreSQL: healthy"
        return 0
    else
        echo -e "${RED}✗${NC} PostgreSQL: unhealthy"
        return 1
    fi
}

check_mongodb() {
    if docker exec whatsapp-mongodb mongosh --eval "db.adminCommand('ping')" &>/dev/null; then
        echo -e "${GREEN}✓${NC} MongoDB: healthy"
        return 0
    else
        echo -e "${RED}✗${NC} MongoDB: unhealthy"
        return 1
    fi
}

check_redis() {
    if docker exec whatsapp-redis redis-cli ping &>/dev/null; then
        echo -e "${GREEN}✓${NC} Redis: healthy"
        return 0
    else
        echo -e "${RED}✗${NC} Redis: unhealthy"
        return 1
    fi
}

check_rabbitmq() {
    if docker exec whatsapp-rabbitmq rabbitmq-diagnostics -q ping &>/dev/null; then
        echo -e "${GREEN}✓${NC} RabbitMQ: healthy"
        return 0
    else
        echo -e "${RED}✗${NC} RabbitMQ: unhealthy"
        return 1
    fi
}

# Count failures
failed=0

echo -e "${BLUE}Infrastructure Services:${NC}"
check_postgres || ((failed++))
check_mongodb || ((failed++))
check_redis || ((failed++))
check_rabbitmq || ((failed++))
echo ""

echo -e "${BLUE}Application Services:${NC}"
check_http "Gateway" "http://localhost:8080/actuator/health" || ((failed++))
check_http "User Service" "http://localhost:8081/actuator/health" || ((failed++))
check_http "Chat Service" "http://localhost:8082/actuator/health" || ((failed++))
check_http "Notification Service" "http://localhost:8084/api/v1/actuator/health" || ((failed++))
check_http "Message Processor" "http://localhost:8085/actuator/health" || ((failed++))
check_http "Scheduled Jobs" "http://localhost:8086/actuator/health" || ((failed++))
echo ""

echo -e "${BLUE}Monitoring Services:${NC}"
check_http "Prometheus" "http://localhost:9090/-/healthy" || ((failed++))
check_http "Grafana" "http://localhost:3000/api/health" || ((failed++))
check_http "Elasticsearch" "http://localhost:9200/_cluster/health" || ((failed++))
check_http "Kibana" "http://localhost:5601/api/status" || ((failed++))
echo ""

echo -e "${BLUE}Reverse Proxy:${NC}"
check_http "Nginx" "http://localhost/health" || ((failed++))
echo ""

# Summary
echo "================================"
if [ $failed -eq 0 ]; then
    echo -e "${GREEN}All services are healthy!${NC}"
    exit 0
else
    echo -e "${RED}$failed service(s) are unhealthy${NC}"
    echo ""
    echo -e "${YELLOW}Troubleshooting:${NC}"
    echo "1. Check logs: docker-compose -f infrastructure/docker/docker-compose.yml logs [service-name]"
    echo "2. Restart service: docker-compose -f infrastructure/docker/docker-compose.yml restart [service-name]"
    echo "3. Check container status: docker ps -a"
    exit 1
fi

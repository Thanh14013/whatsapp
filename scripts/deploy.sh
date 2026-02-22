#!/bin/bash

################################################################################
# WhatsApp Clone - Deployment Script
# Builds and deploys all services using Docker Compose
################################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="infrastructure/docker/docker-compose.yml"
PROJECT_NAME="whatsapp"
BUILD_TIMEOUT=600  # 10 minutes
STARTUP_TIMEOUT=300  # 5 minutes

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}WhatsApp Clone Deployment${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# Function to print colored messages
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker Desktop."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running. Please start Docker Desktop."
        exit 1
    fi
    
    print_success "All prerequisites met"
}

# Clean old containers and volumes
clean_old_deployment() {
    print_info "Cleaning old deployment..."
    
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down -v 2>/dev/null || true
    
    print_success "Old deployment cleaned"
}

# Build all services
build_services() {
    print_info "Building all services (this may take a few minutes)..."
    
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" build --no-cache \
        --build-arg MAVEN_OPTS="-Xmx1024m" \
        || {
        print_error "Build failed"
        exit 1
    }
    
    print_success "All services built successfully"
}

# Start infrastructure services first
start_infrastructure() {
    print_info "Starting infrastructure services..."
    
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d \
        postgres mongodb redis rabbitmq
    
    print_info "Waiting for infrastructure services to be healthy..."
    sleep 20
    
    # Wait for PostgreSQL
    print_info "Waiting for PostgreSQL..."
    timeout 60 bash -c 'until docker exec whatsapp-postgres pg_isready -U whatsapp; do sleep 2; done' || {
        print_error "PostgreSQL failed to start"
        exit 1
    }
    
    # Wait for MongoDB
    print_info "Waiting for MongoDB..."
    timeout 60 bash -c 'until docker exec whatsapp-mongodb mongosh --eval "db.adminCommand(\"ping\")" &>/dev/null; do sleep 2; done' || {
        print_error "MongoDB failed to start"
        exit 1
    }
    
    # Wait for Redis
    print_info "Waiting for Redis..."
    timeout 60 bash -c 'until docker exec whatsapp-redis redis-cli ping &>/dev/null; do sleep 2; done' || {
        print_error "Redis failed to start"
        exit 1
    }
    
    # Wait for RabbitMQ
    print_info "Waiting for RabbitMQ..."
    timeout 90 bash -c 'until docker exec whatsapp-rabbitmq rabbitmq-diagnostics -q ping &>/dev/null; do sleep 3; done' || {
        print_error "RabbitMQ failed to start"
        exit 1
    }
    
    print_success "Infrastructure services are healthy"
}

# Start application services
start_applications() {
    print_info "Starting application services..."
    
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d \
        gateway user-service chat-service notification-service message-processor scheduled-jobs
    
    print_info "Waiting for application services to start..."
    sleep 30
    
    print_success "Application services started"
}

# Start monitoring services
start_monitoring() {
    print_info "Starting monitoring services..."
    
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d \
        prometheus grafana elasticsearch kibana logstash
    
    print_info "Waiting for monitoring services to start..."
    sleep 20
    
    print_success "Monitoring services started"
}

# Start Nginx
start_nginx() {
    print_info "Starting Nginx reverse proxy..."
    
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d nginx
    
    sleep 5
    print_success "Nginx started"
}

# Verify deployment
verify_deployment() {
    print_info "Verifying deployment..."
    
    local failed=0
    
    # Check each service
    services=("gateway" "user-service" "chat-service" "notification-service" "message-processor" "scheduled-jobs")
    
    for service in "${services[@]}"; do
        container="whatsapp-${service}"
        if docker ps --filter "name=$container" --filter "status=running" | grep -q "$container"; then
            print_success "$service is running"
        else
            print_error "$service is not running"
            failed=1
        fi
    done
    
    if [ $failed -eq 1 ]; then
        print_error "Some services failed to start. Check logs with: docker-compose logs"
        return 1
    fi
    
    print_success "All services are running"
}

# Show service URLs
show_urls() {
    echo ""
    print_info "Service URLs:"
    echo -e "${GREEN}API Gateway:${NC}        http://localhost:8080"
    echo -e "${GREEN}User Service:${NC}       http://localhost:8081"
    echo -e "${GREEN}Chat Service:${NC}       http://localhost:8082"
    echo -e "${GREEN}Notification Service:${NC} http://localhost:8084"
    echo -e "${GREEN}Message Processor:${NC}  http://localhost:8085"
    echo -e "${GREEN}Scheduled Jobs:${NC}     http://localhost:8086"
    echo ""
    echo -e "${BLUE}Infrastructure:${NC}"
    echo -e "${GREEN}PostgreSQL:${NC}         localhost:5432 (user: whatsapp, db: whatsapp_db)"
    echo -e "${GREEN}MongoDB:${NC}            localhost:27017 (user: whatsapp, db: whatsapp_chat)"
    echo -e "${GREEN}Redis:${NC}              localhost:6379"
    echo -e "${GREEN}RabbitMQ:${NC}           http://localhost:15672 (user: whatsapp, pass: whatsapp123)"
    echo ""
    echo -e "${BLUE}Monitoring:${NC}"
    echo -e "${GREEN}Prometheus:${NC}         http://localhost:9090"
    echo -e "${GREEN}Grafana:${NC}            http://localhost:3000 (admin/admin123)"
    echo -e "${GREEN}Kibana:${NC}             http://localhost:5601"
    echo ""
    echo -e "${BLUE}Access:${NC}"
    echo -e "${GREEN}Nginx (HTTP):${NC}       http://localhost"
    echo ""
}

# Main deployment flow
main() {
    # Parse arguments
    SKIP_BUILD=false
    CLEAN=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-build)
                SKIP_BUILD=true
                shift
                ;;
            --clean)
                CLEAN=true
                shift
                ;;
            --help)
                echo "Usage: $0 [OPTIONS]"
                echo ""
                echo "Options:"
                echo "  --skip-build    Skip building images (use existing)"
                echo "  --clean         Remove all containers and volumes before deploy"
                echo "  --help          Show this help message"
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    # Execute deployment steps
    check_prerequisites
    
    if [ "$CLEAN" = true ]; then
        clean_old_deployment
    fi
    
    if [ "$SKIP_BUILD" = false ]; then
        build_services
    fi
    
    start_infrastructure
    start_applications
    start_monitoring
    start_nginx
    
    if verify_deployment; then
        echo ""
        print_success "Deployment completed successfully!"
        show_urls
        
        echo ""
        print_info "To view logs: docker-compose -f $COMPOSE_FILE logs -f [service-name]"
        print_info "To stop all services: docker-compose -f $COMPOSE_FILE down"
        print_info "To stop and remove volumes: docker-compose -f $COMPOSE_FILE down -v"
    else
        print_error "Deployment verification failed"
        print_info "Check logs with: docker-compose -f $COMPOSE_FILE logs"
        exit 1
    fi
}

# Run main function
main "$@"

#!/bin/bash

################################################################################
# WhatsApp Clone - Start Script
# Quick start for all services
################################################################################

set -e

COMPOSE_FILE="infrastructure/docker/docker-compose.yml"
PROJECT_NAME="whatsapp"

echo "Starting WhatsApp Clone services..."
echo ""

docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d

echo ""
echo "Services are starting. This may take a minute..."
echo "Check status with: ./scripts/health-check.sh"
echo "View logs with: ./scripts/logs.sh [service-name]"

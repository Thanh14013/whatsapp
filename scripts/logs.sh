#!/bin/bash

################################################################################
# WhatsApp Clone - Logs Script
# View logs from services
################################################################################

COMPOSE_FILE="infrastructure/docker/docker-compose.yml"
PROJECT_NAME="whatsapp"

if [ -z "$1" ]; then
    echo "Showing logs from all services (press Ctrl+C to exit)..."
    echo ""
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f --tail=100
else
    echo "Showing logs from $1 (press Ctrl+C to exit)..."
    echo ""
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f --tail=100 "$1"
fi

#!/bin/bash

################################################################################
# WhatsApp Clone - Stop Script
# Stops all services
################################################################################

set -e

COMPOSE_FILE="infrastructure/docker/docker-compose.yml"
PROJECT_NAME="whatsapp"

echo "Stopping WhatsApp Clone services..."
echo ""

docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" stop

echo ""
echo "All services stopped."
echo "To start again: ./scripts/start.sh"
echo "To remove containers: ./scripts/clean.sh"

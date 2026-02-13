#!/bin/bash

################################################################################
# WhatsApp Clone - Clean Script
# Removes all containers, networks, and optionally volumes
################################################################################

set -e

COMPOSE_FILE="infrastructure/docker/docker-compose.yml"
PROJECT_NAME="whatsapp"

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${YELLOW}WARNING: This will remove all WhatsApp Clone containers and networks.${NC}"
echo ""

if [ "$1" == "--volumes" ] || [ "$1" == "-v" ]; then
    echo -e "${RED}This will also PERMANENTLY DELETE all data (databases, volumes).${NC}"
    echo ""
    read -p "Are you sure? Type 'yes' to continue: " confirm
    
    if [ "$confirm" != "yes" ]; then
        echo "Aborted."
        exit 0
    fi
    
    echo ""
    echo "Removing containers, networks, and volumes..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down -v
    
    echo ""
    echo -e "${GREEN}All containers, networks, and volumes removed.${NC}"
else
    read -p "Continue? (y/n): " confirm
    
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "Aborted."
        exit 0
    fi
    
    echo ""
    echo "Removing containers and networks..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down
    
    echo ""
    echo -e "${GREEN}Containers and networks removed. Volumes preserved.${NC}"
    echo "To also remove volumes: ./scripts/clean.sh --volumes"
fi

echo ""
echo "To start fresh: ./scripts/deploy.sh"

#!/bin/bash

################################################################################
# WhatsApp Clone - Database Migration Script
# Handles PostgreSQL and MongoDB schema initialization/migration
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Database credentials
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-whatsapp}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-whatsapp123}"
POSTGRES_DB="${POSTGRES_DB:-whatsapp_db}"

MONGODB_HOST="${MONGODB_HOST:-localhost}"
MONGODB_PORT="${MONGODB_PORT:-27017}"
MONGODB_USER="${MONGODB_USER:-whatsapp}"
MONGODB_PASSWORD="${MONGODB_PASSWORD:-whatsapp123}"
MONGODB_DB="${MONGODB_DB:-whatsapp_chat}"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Database Migration${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

# Function to print messages
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Wait for PostgreSQL
wait_postgres() {
    print_info "Waiting for PostgreSQL to be ready..."
    
    timeout 60 bash -c "until docker exec whatsapp-postgres pg_isready -U $POSTGRES_USER; do sleep 2; done" || {
        print_error "PostgreSQL is not ready"
        exit 1
    }
    
    print_success "PostgreSQL is ready"
}

# Wait for MongoDB
wait_mongodb() {
    print_info "Waiting for MongoDB to be ready..."
    
    timeout 60 bash -c 'until docker exec whatsapp-mongodb mongosh --eval "db.adminCommand(\"ping\")" &>/dev/null; do sleep 2; done' || {
        print_error "MongoDB is not ready"
        exit 1
    }
    
    print_success "MongoDB is ready"
}

# Run PostgreSQL migrations using Flyway (via user service)
migrate_postgres() {
    print_info "Running PostgreSQL migrations..."
    
    # Check if user service is running (it handles Flyway migrations on startup)
    if docker ps --filter "name=whatsapp-user-service" --filter "status=running" | grep -q whatsapp-user-service; then
        print_info "User service will handle PostgreSQL migrations on startup via Flyway"
        print_success "PostgreSQL migration delegated to User Service"
    else
        # Manual migration if needed
        print_info "Executing manual PostgreSQL schema initialization..."
        
        # Create databases if not exist
        docker exec -i whatsapp-postgres psql -U "$POSTGRES_USER" -d postgres <<EOF
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = '$POSTGRES_DB') THEN
        CREATE DATABASE $POSTGRES_DB;
    END IF;
END
\$\$;
EOF
        
        # Connect to the database and create basic schema
        docker exec -i whatsapp-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<'EOF'
-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    profile_picture_url TEXT,
    status VARCHAR(500),
    last_seen TIMESTAMP,
    is_online BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    name VARCHAR(255),
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Conversation participants
CREATE TABLE IF NOT EXISTS conversation_participants (
    id SERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER' CHECK (role IN ('ADMIN', 'MEMBER')),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(conversation_id, user_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_conversations_created_by ON conversations(created_by);
CREATE INDEX IF NOT EXISTS idx_participants_conversation ON conversation_participants(conversation_id);
CREATE INDEX IF NOT EXISTS idx_participants_user ON conversation_participants(user_id);

EOF
        
        print_success "PostgreSQL schema initialized"
    fi
}

# Initialize MongoDB collections and indexes
migrate_mongodb() {
    print_info "Initializing MongoDB collections and indexes..."
    
    docker exec whatsapp-mongodb mongosh -u "$MONGODB_USER" -p "$MONGODB_PASSWORD" --authenticationDatabase admin "$MONGODB_DB" <<'EOF'

// Messages collection
db.createCollection("messages", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["conversationId", "senderId", "content", "timestamp"],
            properties: {
                conversationId: { bsonType: "string" },
                senderId: { bsonType: "string" },
                content: { bsonType: "string" },
                timestamp: { bsonType: "date" },
                type: { enum: ["TEXT", "IMAGE", "VIDEO", "AUDIO", "FILE", "LOCATION"] },
                status: { enum: ["SENT", "DELIVERED", "READ"] },
                metadata: { bsonType: "object" }
            }
        }
    }
});

// Create indexes for messages
db.messages.createIndex({ conversationId: 1, timestamp: -1 });
db.messages.createIndex({ senderId: 1 });
db.messages.createIndex({ status: 1 });
db.messages.createIndex({ "metadata.replyToId": 1 });

// Message delivery tracking
db.createCollection("message_deliveries", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["messageId", "userId", "status"],
            properties: {
                messageId: { bsonType: "string" },
                userId: { bsonType: "string" },
                status: { enum: ["SENT", "DELIVERED", "READ"] },
                timestamp: { bsonType: "date" }
            }
        }
    }
});

db.message_deliveries.createIndex({ messageId: 1, userId: 1 }, { unique: true });
db.message_deliveries.createIndex({ userId: 1, status: 1 });

// Media attachments
db.createCollection("attachments");
db.attachments.createIndex({ messageId: 1 });
db.attachments.createIndex({ uploadedBy: 1 });

// Inbox cache (for quick message preview)
db.createCollection("inbox_cache");
db.inbox_cache.createIndex({ userId: 1, conversationId: 1 }, { unique: true });
db.inbox_cache.createIndex({ userId: 1, lastMessageTime: -1 });

print("MongoDB collections and indexes created successfully");

EOF
    
    if [ $? -eq 0 ]; then
        print_success "MongoDB initialized successfully"
    else
        print_error "MongoDB initialization failed"
        exit 1
    fi
}

# Main execution
main() {
    case "${1:-all}" in
        postgres)
            wait_postgres
            migrate_postgres
            ;;
        mongodb)
            wait_mongodb
            migrate_mongodb
            ;;
        all)
            wait_postgres
            wait_mongodb
            migrate_postgres
            migrate_mongodb
            ;;
        --help)
            echo "Usage: $0 [postgres|mongodb|all]"
            echo ""
            echo "Options:"
            echo "  postgres   Run only PostgreSQL migrations"
            echo "  mongodb    Run only MongoDB initialization"
            echo "  all        Run all migrations (default)"
            echo ""
            echo "Environment variables:"
            echo "  POSTGRES_HOST, POSTGRES_PORT, POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB"
            echo "  MONGODB_HOST, MONGODB_PORT, MONGODB_USER, MONGODB_PASSWORD, MONGODB_DB"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
    
    echo ""
    print_success "Database migration completed successfully!"
}

main "$@"

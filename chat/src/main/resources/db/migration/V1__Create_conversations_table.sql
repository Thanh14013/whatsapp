-- ===================================================================
-- WhatsApp Clone - Chat Service
-- Create conversations table
-- ===================================================================

CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    description TEXT,
    avatar_url VARCHAR(500),
    last_message_id VARCHAR(255),
    last_message_timestamp TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_conversations_type ON conversations(type);
CREATE INDEX idx_conversations_active ON conversations(active);
CREATE INDEX idx_conversations_last_message_timestamp ON conversations(last_message_timestamp DESC);

-- Comments
COMMENT ON TABLE conversations IS 'Stores conversation metadata for one-to-one and group chats';
COMMENT ON COLUMN conversations.type IS 'Conversation type: ONE_TO_ONE, GROUP, or BROADCAST';
COMMENT ON COLUMN conversations.last_message_id IS 'Reference to the last message in the conversation';

-- ===================================================================
-- WhatsApp Clone - Chat Service
-- Create conversation_participants table
-- ===================================================================

CREATE TABLE IF NOT EXISTS conversation_participants (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    unread_count INTEGER NOT NULL DEFAULT 0,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    
    CONSTRAINT fk_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES conversations(id)
        ON DELETE CASCADE,
    
    CONSTRAINT unique_conversation_user
        UNIQUE (conversation_id, user_id)
);

-- Indexes for performance
CREATE INDEX idx_conversation_participants_conversation ON conversation_participants(conversation_id);
CREATE INDEX idx_conversation_participants_user ON conversation_participants(user_id);
CREATE INDEX idx_conversation_participants_unread ON conversation_participants(user_id, unread_count) WHERE unread_count > 0;
CREATE INDEX idx_conversation_participants_admin ON conversation_participants(conversation_id, is_admin) WHERE is_admin = TRUE;

-- Comments
COMMENT ON TABLE conversation_participants IS 'Stores participants of conversations with their roles and unread counts';
COMMENT ON COLUMN conversation_participants.is_admin IS 'Indicates if participant is admin (for group conversations)';
COMMENT ON COLUMN conversation_participants.unread_count IS 'Number of unread messages for this participant';
COMMENT ON COLUMN conversation_participants.left_at IS 'Timestamp when participant left the conversation (for groups)';

-- ===================================================================
-- WhatsApp Clone - Chat Service
-- Create message_delivery_tracking table
-- ===================================================================

CREATE TABLE IF NOT EXISTS message_delivery_tracking (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL UNIQUE,
    conversation_id VARCHAR(255) NOT NULL,
    sender_id VARCHAR(255) NOT NULL,
    receiver_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_message_delivery_message ON message_delivery_tracking(message_id);
CREATE INDEX idx_message_delivery_conversation ON message_delivery_tracking(conversation_id);
CREATE INDEX idx_message_delivery_sender ON message_delivery_tracking(sender_id);
CREATE INDEX idx_message_delivery_receiver ON message_delivery_tracking(receiver_id);
CREATE INDEX idx_message_delivery_status ON message_delivery_tracking(status);
CREATE INDEX idx_message_delivery_sent_at ON message_delivery_tracking(sent_at DESC);

-- Comments
COMMENT ON TABLE message_delivery_tracking IS 'Tracks message delivery status for reporting and analytics';
COMMENT ON COLUMN message_delivery_tracking.status IS 'Message status: SENT, DELIVERED, or READ';
COMMENT ON COLUMN message_delivery_tracking.sent_at IS 'When the message was sent';
COMMENT ON COLUMN message_delivery_tracking.delivered_at IS 'When the message was delivered to recipient';
COMMENT ON COLUMN message_delivery_tracking.read_at IS 'When the message was read by recipient';

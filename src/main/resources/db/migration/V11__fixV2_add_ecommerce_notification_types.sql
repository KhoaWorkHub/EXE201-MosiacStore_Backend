-- Document all supported notification types
COMMENT ON TABLE notifications IS 'System notifications including types: CHAT_MESSAGE, SYSTEM, ORDER_STATUS, PAYMENT, SUPPORT_REQUEST, NEW_ORDER, CART_ACTIVITY, ABANDONED_CART';

-- Create index for improved performance when querying specific notification types
CREATE INDEX IF NOT EXISTS idx_notifications_by_type
    ON notifications (type);

-- Add column to link notifications directly to relevant entities
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS source_entity_id UUID;

-- Create index for faster entity-specific notification queries
CREATE INDEX IF NOT EXISTS idx_notifications_by_source
    ON notifications (source_entity_id);
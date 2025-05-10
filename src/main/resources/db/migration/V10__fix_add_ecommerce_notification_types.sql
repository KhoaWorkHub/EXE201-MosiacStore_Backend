-- Document the notification types in the database
COMMENT ON TABLE notifications IS 'System notifications including: CHAT_MESSAGE, SYSTEM, ORDER_STATUS, PAYMENT, SUPPORT_REQUEST, NEW_ORDER, CART_ACTIVITY, ABANDONED_CART';

-- Create indexes for better performance when querying by these specific types
CREATE INDEX IF NOT EXISTS idx_notifications_ecommerce_types
    ON notifications (type)
    WHERE type IN ('NEW_ORDER', 'CART_ACTIVITY', 'ABANDONED_CART');

-- Add column for notification grouping by order/cart ID for better filtering
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS related_entity_id UUID;

-- Add index for faster querying of notifications by their related entity
CREATE INDEX IF NOT EXISTS idx_notifications_related_entity
    ON notifications (related_entity_id);
ALTER TABLE notifications
DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
        CHECK (type IN ('CHAT_MESSAGE', 'SYSTEM', 'ORDER_STATUS', 'PAYMENT', 'SUPPORT_REQUEST', 'NEW_ORDER', 'CART_ACTIVITY', 'ABANDONED_CART'));
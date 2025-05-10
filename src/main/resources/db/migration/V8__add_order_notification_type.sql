
-- Create index on notification table for better performance when querying by type
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications (type);

-- Create index for recipient user optimization
CREATE INDEX IF NOT EXISTS idx_notifications_user_type ON notifications (user_id, type);

-- Add column for improved notification grouping
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS notification_group VARCHAR(50);
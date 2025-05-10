-- Create chat_rooms table
CREATE TABLE IF NOT EXISTS chat_rooms (
                            room_id UUID PRIMARY KEY,
                            name VARCHAR(100),
                            type VARCHAR(20) NOT NULL,
                            is_active BOOLEAN DEFAULT TRUE,
                            created_at TIMESTAMP,
                            updated_at TIMESTAMP
);

-- Create chat_room_participants table (junction table)
CREATE TABLE IF NOT EXISTS chat_room_participants (
                                        room_id UUID NOT NULL,
                                        user_id UUID NOT NULL,
                                        PRIMARY KEY (room_id, user_id),
                                        FOREIGN KEY (room_id) REFERENCES chat_rooms (room_id) ON DELETE CASCADE,
                                        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- Create chat_messages table
CREATE TABLE IF NOT EXISTS chat_messages (
                               message_id UUID PRIMARY KEY,
                               room_id UUID NOT NULL,
                               sender_id UUID,
                               content TEXT,
                               status VARCHAR(20) NOT NULL,
                               priority VARCHAR(20),
                               requires_attention BOOLEAN DEFAULT FALSE,
                               is_system_message BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMP,
                               updated_at TIMESTAMP,
                               FOREIGN KEY (room_id) REFERENCES chat_rooms (room_id) ON DELETE CASCADE,
                               FOREIGN KEY (sender_id) REFERENCES users (user_id) ON DELETE SET NULL
);

-- Create message_read_statuses table
CREATE TABLE IF NOT EXISTS message_read_statuses (
                                       read_status_id UUID PRIMARY KEY,
                                       message_id UUID NOT NULL,
                                       user_id UUID NOT NULL,
                                       is_read BOOLEAN DEFAULT FALSE,
                                       created_at TIMESTAMP,
                                       updated_at TIMESTAMP,
                                       FOREIGN KEY (message_id) REFERENCES chat_messages (message_id) ON DELETE CASCADE,
                                       FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- Create chat_attachments table
CREATE TABLE IF NOT EXISTS chat_attachments (
                                  attachment_id UUID PRIMARY KEY,
                                  message_id UUID NOT NULL,
                                  file_url VARCHAR(512) NOT NULL,
                                  file_name VARCHAR(255) NOT NULL,
                                  file_type VARCHAR(100),
                                  file_size BIGINT,
                                  is_image BOOLEAN DEFAULT FALSE,
                                  thumbnail_url VARCHAR(512),
                                  created_at TIMESTAMP,
                                  updated_at TIMESTAMP,
                                  FOREIGN KEY (message_id) REFERENCES chat_messages (message_id) ON DELETE CASCADE
);

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
                               notification_id UUID PRIMARY KEY,
                               user_id UUID NOT NULL,
                               title VARCHAR(200) NOT NULL,
                               content TEXT,
                               type VARCHAR(50) NOT NULL,
                               source_type VARCHAR(50),
                               source_id VARCHAR(100),
                               is_read BOOLEAN DEFAULT FALSE,
                               action_url VARCHAR(512),
                               created_at TIMESTAMP,
                               updated_at TIMESTAMP,
                               FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- Add indexes for better performance
CREATE INDEX idx_chat_messages_room_id ON chat_messages (room_id);
CREATE INDEX idx_chat_messages_sender_id ON chat_messages (sender_id);
CREATE INDEX idx_message_read_statuses_message_id ON message_read_statuses (message_id);
CREATE INDEX idx_message_read_statuses_user_id ON message_read_statuses (user_id);
CREATE INDEX idx_chat_attachments_message_id ON chat_attachments (message_id);
CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_type ON notifications (type);
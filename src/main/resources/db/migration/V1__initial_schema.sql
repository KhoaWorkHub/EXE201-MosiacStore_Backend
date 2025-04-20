-- Initial database schema

-- User table
CREATE TABLE users (
                       user_id UUID PRIMARY KEY,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password_hash VARCHAR(256) NOT NULL,
                       full_name VARCHAR(100) NOT NULL,
                       phone_number VARCHAR(15),
                       role VARCHAR(20) NOT NULL,
                       status VARCHAR(20) NOT NULL,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP NOT NULL,
                       last_login TIMESTAMP,
                       failed_login_attempts INT DEFAULT 0,
                       locked_until TIMESTAMP,
                       provider VARCHAR(20),
                       provider_id VARCHAR(100)
);

-- User token table for refresh tokens
CREATE TABLE user_tokens (
                             token_id UUID PRIMARY KEY,
                             user_id UUID NOT NULL REFERENCES users(user_id),
                             refresh_token VARCHAR(256) NOT NULL,
                             token_type VARCHAR(20) NOT NULL,
                             expires_at TIMESTAMP NOT NULL,
                             revoked BOOLEAN DEFAULT FALSE,
                             updated_at TIMESTAMP NOT NULL,
                             created_at TIMESTAMP NOT NULL
);

-- Initial admin user (password: admin123)
INSERT INTO users (
    user_id,
    email,
    password_hash,
    full_name,
    role,
    status,
    created_at,
    updated_at
) VALUES (
             '123e4567-e89b-12d3-a456-426614174000',
             'admin@example.com',
             '$2a$10$rPIuQQUnZQufJ4qT7QEkZOD8u5iUG6RVwGGJXGvIQIzez6MgJz3PW',
             'Admin User',
             'ADMIN',
             'ACTIVE',
             NOW(),
             NOW()
         );
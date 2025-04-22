-- Enum types
CREATE TYPE campaign_type AS ENUM (
    'DISCOUNT_PERCENTAGE',
    'DISCOUNT_FIXED_AMOUNT',
    'BUY_X_GET_Y',
    'FREE_SHIPPING',
    'BUNDLE_DISCOUNT',
    'QR_SCAN_REWARD',
    'REGION_SPECIFIC'
);

CREATE TYPE rule_type AS ENUM (
    'MINIMUM_ORDER_VALUE',
    'MINIMUM_QUANTITY',
    'REGION_ID',
    'PRODUCT_CATEGORY',
    'CUSTOMER_TYPE',
    'QR_SCAN_COUNT',
    'FIRST_ORDER'
);

-- Campaigns table
CREATE TABLE campaigns (
                           id UUID PRIMARY KEY,
                           name VARCHAR(100) NOT NULL,
                           description TEXT,
                           code VARCHAR(50) UNIQUE,
                           start_date TIMESTAMP NOT NULL,
                           end_date TIMESTAMP NOT NULL,
                           is_active BOOLEAN DEFAULT TRUE,
                           campaign_type campaign_type NOT NULL,
                           priority INTEGER DEFAULT 0,
                           max_usage INTEGER,
                           current_usage INTEGER DEFAULT 0,
                           created_at TIMESTAMP,
                           updated_at TIMESTAMP
);

-- Campaign rules table
CREATE TABLE campaign_rules (
                                id UUID PRIMARY KEY,
                                campaign_id UUID NOT NULL,
                                rule_type rule_type NOT NULL,
                                value VARCHAR(255) NOT NULL,
                                operator VARCHAR(20),
                                created_at TIMESTAMP,
                                updated_at TIMESTAMP,
                                FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

-- Campaign products table
CREATE TABLE campaign_products (
                                   id UUID PRIMARY KEY,
                                   campaign_id UUID NOT NULL,
                                   product_id UUID NOT NULL,
                                   discount_value DECIMAL(10,2),
                                   is_gift BOOLEAN DEFAULT FALSE,
                                   created_at TIMESTAMP,
                                   updated_at TIMESTAMP,
                                   FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
                                   FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- Campaign usages table
CREATE TABLE campaign_usages (
                                 id UUID PRIMARY KEY,
                                 campaign_id UUID NOT NULL,
                                 user_id UUID,
                                 order_id UUID,
                                 usage_date TIMESTAMP NOT NULL,
                                 discount_amount DECIMAL(10,2),
                                 created_at TIMESTAMP,
                                 updated_at TIMESTAMP,
                                 FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
                                 FOREIGN KEY (user_id) REFERENCES users(user_id),
                                 FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- Index for faster lookups
CREATE INDEX idx_campaign_code ON campaigns(code);
CREATE INDEX idx_campaign_dates ON campaigns(start_date, end_date);
CREATE INDEX idx_campaign_product ON campaign_products(product_id);
CREATE INDEX idx_campaign_usage_user ON campaign_usages(user_id);
CREATE INDEX idx_campaign_usage_order ON campaign_usages(order_id);

-- Add discount_info column to order_items to track applied discounts
ALTER TABLE order_items ADD COLUMN campaign_id UUID;
ALTER TABLE order_items ADD COLUMN discount_amount DECIMAL(10,2) DEFAULT 0;
ALTER TABLE order_items ADD CONSTRAINT fk_order_item_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id);

-- Add campaign reference to orders table
ALTER TABLE orders ADD COLUMN campaign_id UUID;
ALTER TABLE orders ADD COLUMN total_discount DECIMAL(10,2) DEFAULT 0;
ALTER TABLE orders ADD CONSTRAINT fk_order_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id);
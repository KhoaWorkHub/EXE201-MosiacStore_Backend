-- Regions
CREATE TABLE regions (
                         region_id UUID PRIMARY KEY,
                         name VARCHAR(100) NOT NULL,
                         slug VARCHAR(150) NOT NULL UNIQUE,
                         description TEXT,
                         image_url VARCHAR(255),
                         active BOOLEAN DEFAULT TRUE,
                         created_at TIMESTAMP,
                         updated_at TIMESTAMP
);

-- Product Categories
CREATE TABLE product_categories (
                                    category_id UUID PRIMARY KEY,
                                    name VARCHAR(100) NOT NULL,
                                    slug VARCHAR(150) NOT NULL UNIQUE,
                                    description TEXT,
                                    parent_id UUID,
                                    image_url VARCHAR(255),
                                    display_order INT,
                                    active BOOLEAN DEFAULT TRUE,
                                    created_at TIMESTAMP,
                                    updated_at TIMESTAMP,
                                    FOREIGN KEY (parent_id) REFERENCES product_categories(category_id)
);

-- Products
CREATE TABLE products (
                          product_id UUID PRIMARY KEY,
                          category_id UUID,
                          name VARCHAR(150) NOT NULL,
                          slug VARCHAR(200) NOT NULL UNIQUE,
                          description TEXT,
                          short_description VARCHAR(255),
                          price DECIMAL(10,2) NOT NULL,
                          original_price DECIMAL(10,2),
                          stock_quantity INT,
                          sku VARCHAR(50),
                          region_id UUID,
                          active BOOLEAN DEFAULT TRUE,
                          featured BOOLEAN DEFAULT FALSE,
                          view_count INT DEFAULT 0,
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP,
                          FOREIGN KEY (category_id) REFERENCES product_categories(category_id),
                          FOREIGN KEY (region_id) REFERENCES regions(region_id)
);

-- Product Variants
CREATE TABLE product_variants (
                                  variant_id UUID PRIMARY KEY,
                                  product_id UUID NOT NULL,
                                  size VARCHAR(10),
                                  color VARCHAR(50),
                                  price_adjustment DECIMAL(10,2) DEFAULT 0,
                                  stock_quantity INT,
                                  sku_variant VARCHAR(60),
                                  active BOOLEAN DEFAULT TRUE,
                                  created_at TIMESTAMP,
                                  updated_at TIMESTAMP,
                                  FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- Product Images
CREATE TABLE product_images (
                                image_id UUID PRIMARY KEY,
                                product_id UUID NOT NULL,
                                variant_id UUID,
                                image_url VARCHAR(255) NOT NULL,
                                alt_text VARCHAR(150),
                                display_order INT,
                                is_primary BOOLEAN DEFAULT FALSE,
                                created_at TIMESTAMP,
                                updated_at TIMESTAMP,
                                FOREIGN KEY (product_id) REFERENCES products(product_id),
                                FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id)
);

-- QR Codes
CREATE TABLE qr_codes (
                          qr_id UUID PRIMARY KEY,
                          product_id UUID,
                          qr_image_url VARCHAR(255),
                          qr_data VARCHAR(500),
                          redirect_url VARCHAR(500),
                          scan_count INT DEFAULT 0,
                          active BOOLEAN DEFAULT TRUE,
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP,
                          FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- User roles & statuses are ENUMs in Java, need to create type in PostgreSQL
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'ADMIN', 'STAFF');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'BANNED');

-- Update users table if needed (uncomment only if you need to modify existing table)
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS role user_role;
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS status user_status;

-- Token type enum
CREATE TYPE token_type AS ENUM ('REFRESH', 'RESET_PASSWORD');

-- Provinces
CREATE TABLE provinces (
                           province_code VARCHAR(10) PRIMARY KEY,
                           name VARCHAR(100) NOT NULL,
                           region VARCHAR(50),
                           created_at TIMESTAMP,
                           updated_at TIMESTAMP
);

-- Districts
CREATE TABLE districts (
                           district_code VARCHAR(10) PRIMARY KEY,
                           province_code VARCHAR(10) NOT NULL,
                           name VARCHAR(100) NOT NULL,
                           created_at TIMESTAMP,
                           updated_at TIMESTAMP,
                           FOREIGN KEY (province_code) REFERENCES provinces(province_code)
);

-- Wards
CREATE TABLE wards (
                       ward_code VARCHAR(10) PRIMARY KEY,
                       district_code VARCHAR(10) NOT NULL,
                       name VARCHAR(100) NOT NULL,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP,
                       FOREIGN KEY (district_code) REFERENCES districts(district_code)
);

-- Addresses
CREATE TABLE addresses (
                           address_id UUID PRIMARY KEY,
                           user_id UUID NOT NULL,
                           recipient_name VARCHAR(100) NOT NULL,
                           phone VARCHAR(15) NOT NULL,
                           province_code VARCHAR(10),
                           district_code VARCHAR(10),
                           ward_code VARCHAR(10),
                           street_address VARCHAR(255) NOT NULL,
                           is_default BOOLEAN DEFAULT FALSE,
                           created_at TIMESTAMP,
                           updated_at TIMESTAMP,
                           FOREIGN KEY (user_id) REFERENCES users(user_id),
                           FOREIGN KEY (province_code) REFERENCES provinces(province_code),
                           FOREIGN KEY (district_code) REFERENCES districts(district_code),
                           FOREIGN KEY (ward_code) REFERENCES wards(ward_code)
);

-- Tour Guides
CREATE TABLE tour_guides (
                             guide_id UUID PRIMARY KEY,
                             region_id UUID,
                             product_id UUID,
                             title VARCHAR(200) NOT NULL,
                             slug VARCHAR(250) NOT NULL UNIQUE,
                             content TEXT,
                             thumbnail_url VARCHAR(255),
                             author_id UUID,
                             view_count INT DEFAULT 0,
                             published BOOLEAN DEFAULT FALSE,
                             published_at TIMESTAMP,
                             featured BOOLEAN DEFAULT FALSE,
                             meta_title VARCHAR(200),
                             meta_description VARCHAR(500),
                             created_at TIMESTAMP,
                             updated_at TIMESTAMP,
                             FOREIGN KEY (region_id) REFERENCES regions(region_id),
                             FOREIGN KEY (product_id) REFERENCES products(product_id),
                             FOREIGN KEY (author_id) REFERENCES users(user_id)
);

-- Tour Guide Images
CREATE TABLE tour_guide_images (
                                   image_id UUID PRIMARY KEY,
                                   guide_id UUID NOT NULL,
                                   image_url VARCHAR(255) NOT NULL,
                                   caption VARCHAR(255),
                                   display_order INT,
                                   created_at TIMESTAMP,
                                   updated_at TIMESTAMP,
                                   FOREIGN KEY (guide_id) REFERENCES tour_guides(guide_id)
);

-- Carts
CREATE TABLE carts (
                       cart_id UUID PRIMARY KEY,
                       user_id UUID,
                       guest_id VARCHAR(100),
                       expired_at TIMESTAMP,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP,
                       FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Cart Items
CREATE TABLE cart_items (
                            cart_item_id UUID PRIMARY KEY,
                            cart_id UUID NOT NULL,
                            product_id UUID NOT NULL,
                            variant_id UUID,
                            quantity INT NOT NULL,
                            price_snapshot DECIMAL(10,2) NOT NULL,
                            added_at TIMESTAMP NOT NULL,
                            updated_at TIMESTAMP,
                            created_at TIMESTAMP,
                            FOREIGN KEY (cart_id) REFERENCES carts(cart_id),
                            FOREIGN KEY (product_id) REFERENCES products(product_id),
                            FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id)
);

-- Order status enum
CREATE TYPE order_status AS ENUM ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPING', 'DELIVERED', 'CANCELLED');

-- Orders
CREATE TABLE orders (
                        order_id UUID PRIMARY KEY,
                        order_number VARCHAR(50) NOT NULL UNIQUE,
                        user_id UUID,
                        status order_status NOT NULL,
                        total_product_amount DECIMAL(10,2) NOT NULL,
                        shipping_fee DECIMAL(10,2),
                        total_amount DECIMAL(10,2) NOT NULL,
                        recipient_name VARCHAR(100) NOT NULL,
                        recipient_phone VARCHAR(15) NOT NULL,
                        shipping_address_id UUID,
                        shipping_address_snapshot TEXT,
                        note TEXT,
                        payment_due TIMESTAMP,
                        cancelled_reason VARCHAR(255),
                        admin_note TEXT,
                        created_at TIMESTAMP,
                        updated_at TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(user_id),
                        FOREIGN KEY (shipping_address_id) REFERENCES addresses(address_id)
);

-- Order Items
CREATE TABLE order_items (
                             order_item_id UUID PRIMARY KEY,
                             order_id UUID NOT NULL,
                             product_id UUID NOT NULL,
                             variant_id UUID,
                             product_name_snapshot VARCHAR(150) NOT NULL,
                             variant_info_snapshot VARCHAR(150),
                             price_snapshot DECIMAL(10,2) NOT NULL,
                             quantity INT NOT NULL,
                             subtotal DECIMAL(10,2) NOT NULL,
                             created_at TIMESTAMP,
                             updated_at TIMESTAMP,
                             FOREIGN KEY (order_id) REFERENCES orders(order_id),
                             FOREIGN KEY (product_id) REFERENCES products(product_id),
                             FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id)
);

-- Payment method enum
CREATE TYPE payment_method AS ENUM ('BANK_TRANSFER', 'COD', 'VNPAY', 'MOMO');

-- Payment status enum
CREATE TYPE payment_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED');

-- Payments
CREATE TABLE payments (
                          payment_id UUID PRIMARY KEY,
                          order_id UUID NOT NULL,
                          payment_method payment_method NOT NULL,
                          amount DECIMAL(10,2) NOT NULL,
                          status payment_status NOT NULL,
                          transaction_reference VARCHAR(100),
                          payment_proof_url VARCHAR(255),
                          payment_date TIMESTAMP,
                          bank_account_number VARCHAR(30),
                          bank_name VARCHAR(100),
                          payment_note TEXT,
                          admin_id UUID,
                          admin_note TEXT,
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP,
                          FOREIGN KEY (order_id) REFERENCES orders(order_id),
                          FOREIGN KEY (admin_id) REFERENCES users(user_id)
);

-- Invoices
CREATE TABLE invoices (
                          invoice_id UUID PRIMARY KEY,
                          order_id UUID NOT NULL,
                          invoice_number VARCHAR(50) NOT NULL UNIQUE,
                          pdf_url VARCHAR(255),
                          issued_date TIMESTAMP NOT NULL,
                          sent BOOLEAN DEFAULT FALSE,
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP,
                          FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- QR Scans
CREATE TABLE qr_scans (
                          scan_id UUID PRIMARY KEY,
                          qr_id UUID NOT NULL,
                          scan_date TIMESTAMP NOT NULL,
                          ip_address VARCHAR(45),
                          user_agent VARCHAR(255),
                          geo_location VARCHAR(100),
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP,
                          FOREIGN KEY (qr_id) REFERENCES qr_codes(qr_id)
);

-- Audit Logs
CREATE TABLE audit_logs (
                            log_id UUID PRIMARY KEY,
                            user_id UUID,
                            action VARCHAR(100) NOT NULL,
                            entity_type VARCHAR(50),
                            entity_id VARCHAR(50),
                            old_value TEXT,
                            new_value TEXT,
                            ip_address VARCHAR(45),
                            user_agent VARCHAR(255),
                            status VARCHAR(20),
                            details TEXT,
                            created_at TIMESTAMP,
                            updated_at TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(user_id)
);
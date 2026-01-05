-- Migration: Company Subscription Items and Payment
-- Version: V13
-- Description: Adds subscription items, payment fields, and delivery address

-- Add payment fields to company_subscriptions
ALTER TABLE company_subscriptions
    ADD COLUMN payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN payment_amount DECIMAL(10, 2),
    ADD COLUMN payment_method VARCHAR(50),
    ADD COLUMN transaction_id VARCHAR(100);

-- Add delivery address to company_orders (allow NULL first, then update, then make NOT NULL)
ALTER TABLE company_orders
    ADD COLUMN delivery_address VARCHAR(500) NULL;

-- Update existing records to have default delivery address
UPDATE company_orders SET delivery_address = 'Office Address' WHERE delivery_address IS NULL;

-- Now make it NOT NULL
ALTER TABLE company_orders
    MODIFY COLUMN delivery_address VARCHAR(500) NOT NULL;

-- Subscription Items table: Menu items selected for a subscription
CREATE TABLE IF NOT EXISTS subscription_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    INDEX idx_subscription_id (subscription_id),
    INDEX idx_menu_item_id (menu_item_id),
    FOREIGN KEY (subscription_id) REFERENCES company_subscriptions(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE,
    UNIQUE KEY unique_subscription_item (subscription_id, menu_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


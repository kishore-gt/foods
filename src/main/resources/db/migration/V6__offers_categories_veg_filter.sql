-- Migration: Offers, Categories, and Veg/Non-Veg Filter
-- Version: V6
-- Description: Adds offers system, food categories, and veg/non-veg filtering

-- Add category and veg/non-veg fields to menu_items
-- Note: Flyway will track this migration, so columns won't be added twice
ALTER TABLE menu_items 
ADD COLUMN category VARCHAR(50) NULL COMMENT 'Food category: DESSERTS, STARTERS, BEVERAGES, SNACKS, etc.',
ADD COLUMN is_veg BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'TRUE for veg, FALSE for non-veg';

-- Add indexes
CREATE INDEX idx_category ON menu_items(category);
CREATE INDEX idx_is_veg ON menu_items(is_veg);

-- Offers table: Restaurant offers and promotions
CREATE TABLE IF NOT EXISTS offer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL COMMENT 'Offer title like "20% OFF" or "Buy 1 Get 1 Free"',
    description VARCHAR(500) COMMENT 'Detailed offer description',
    offer_type VARCHAR(50) NOT NULL COMMENT 'PERCENTAGE_OFF, BUY_ONE_GET_ONE, FLAT_DISCOUNT, etc.',
    discount_value DECIMAL(10,2) COMMENT 'Discount amount or percentage',
    min_order_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Minimum order amount to avail offer',
    max_discount DECIMAL(10,2) COMMENT 'Maximum discount cap',
    applicable_menu_item_ids TEXT COMMENT 'Comma-separated menu item IDs, NULL means all items',
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(500) COMMENT 'Offer poster/banner image',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_restaurant_id (restaurant_id),
    INDEX idx_is_active (is_active),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Update existing menu items to have default category and veg status
UPDATE menu_items SET category = 'STARTERS', is_veg = TRUE WHERE category IS NULL;


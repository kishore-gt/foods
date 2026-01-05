-- Migration V9: Add Cuisine Categories, Ratings, Ordering Modes, and Restaurant Categories
-- This migration is idempotent - checks for existing columns before adding

-- Add cuisine and category fields to restaurants (only if they don't exist)
SET @dbname = DATABASE();
SET @tablename = 'restaurants';

-- Check and add cuisine_type
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'cuisine_type');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN cuisine_type VARCHAR(50) DEFAULT NULL COMMENT ''INDIAN, CHINESE, ITALIAN, AMERICAN, CONTINENTAL, ANDHRA_TELANGANA, NORTH_INDIAN, SOUTH_INDIAN, ARABIAN_TURKISH, BAKERY_DESSERTS''',
    'SELECT ''Column cuisine_type already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add is_pure_veg
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'is_pure_veg');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN is_pure_veg BOOLEAN DEFAULT FALSE',
    'SELECT ''Column is_pure_veg already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add is_cloud_kitchen
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'is_cloud_kitchen');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN is_cloud_kitchen BOOLEAN DEFAULT FALSE',
    'SELECT ''Column is_cloud_kitchen already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add is_family_restaurant
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'is_family_restaurant');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN is_family_restaurant BOOLEAN DEFAULT FALSE',
    'SELECT ''Column is_family_restaurant already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add is_cafe_lounge
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'is_cafe_lounge');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN is_cafe_lounge BOOLEAN DEFAULT FALSE',
    'SELECT ''Column is_cafe_lounge already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add average_rating
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'average_rating');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN average_rating DECIMAL(3,2) DEFAULT 0.00',
    'SELECT ''Column average_rating already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add total_ratings
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'total_ratings');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN total_ratings INT DEFAULT 0',
    'SELECT ''Column total_ratings already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add delivery_time_minutes
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'delivery_time_minutes');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN delivery_time_minutes INT DEFAULT 30',
    'SELECT ''Column delivery_time_minutes already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add min_order_amount
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'min_order_amount');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN min_order_amount DECIMAL(10,2) DEFAULT 0.00',
    'SELECT ''Column min_order_amount already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add delivery_fee
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'delivery_fee');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN delivery_fee DECIMAL(10,2) DEFAULT 0.00',
    'SELECT ''Column delivery_fee already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add is_active
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'is_active');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE restaurants ADD COLUMN is_active BOOLEAN DEFAULT TRUE',
    'SELECT ''Column is_active already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create restaurant_category_tags table for multiple categories per restaurant
CREATE TABLE IF NOT EXISTS restaurant_category_tags (
    restaurant_id BIGINT NOT NULL,
    category_tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (restaurant_id, category_tag),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create ratings and reviews table
CREATE TABLE IF NOT EXISTS restaurant_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text VARCHAR(1000),
    order_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_restaurant_review (user_id, restaurant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create menu_item_reviews table
CREATE TABLE IF NOT EXISTS menu_item_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text VARCHAR(500),
    order_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add ordering mode to multi_order (check if columns exist first)
SET @tablename = 'multi_order';

-- Check and add ordering_mode
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'ordering_mode');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE multi_order ADD COLUMN ordering_mode VARCHAR(20) DEFAULT ''DELIVERY'' COMMENT ''DELIVERY, TAKEAWAY, DINE_IN, PREORDER''',
    'SELECT ''Column ordering_mode already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check and add scheduled_delivery_time
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'scheduled_delivery_time');
SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE multi_order ADD COLUMN scheduled_delivery_time DATETIME DEFAULT NULL COMMENT ''For preorders and scheduled deliveries''',
    'SELECT ''Column scheduled_delivery_time already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create index for faster filtering (only if they don't exist)
-- Note: MySQL will error if index already exists, so we check first
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'restaurants' AND INDEX_NAME = 'idx_restaurant_cuisine');
SET @sql = IF(@idx_exists = 0, 
    'CREATE INDEX idx_restaurant_cuisine ON restaurants(cuisine_type)',
    'SELECT ''Index idx_restaurant_cuisine already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'restaurants' AND INDEX_NAME = 'idx_restaurant_rating');
SET @sql = IF(@idx_exists = 0, 
    'CREATE INDEX idx_restaurant_rating ON restaurants(average_rating DESC)',
    'SELECT ''Index idx_restaurant_rating already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'restaurants' AND INDEX_NAME = 'idx_restaurant_active');
SET @sql = IF(@idx_exists = 0, 
    'CREATE INDEX idx_restaurant_active ON restaurants(is_active)',
    'SELECT ''Index idx_restaurant_active already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'restaurant_reviews' AND INDEX_NAME = 'idx_review_restaurant');
SET @sql = IF(@idx_exists = 0, 
    'CREATE INDEX idx_review_restaurant ON restaurant_reviews(restaurant_id)',
    'SELECT ''Index idx_review_restaurant already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'restaurant_reviews' AND INDEX_NAME = 'idx_review_user');
SET @sql = IF(@idx_exists = 0, 
    'CREATE INDEX idx_review_user ON restaurant_reviews(user_id)',
    'SELECT ''Index idx_review_user already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
                   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'menu_item_reviews' AND INDEX_NAME = 'idx_menu_item_review_item');
SET @sql = IF(@idx_exists = 0, 
    'CREATE INDEX idx_menu_item_review_item ON menu_item_reviews(menu_item_id)',
    'SELECT ''Index idx_menu_item_review_item already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

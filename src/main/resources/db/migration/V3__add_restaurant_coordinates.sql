-- Add latitude and longitude to restaurants table for rider assignment
-- This migration is idempotent - it checks if columns exist before adding

-- Add latitude column if it doesn't exist
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'restaurants' 
    AND COLUMN_NAME = 'latitude'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE restaurants ADD COLUMN latitude DECIMAL(10, 8) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add longitude column if it doesn't exist
SET @col_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'restaurants' 
    AND COLUMN_NAME = 'longitude'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE restaurants ADD COLUMN longitude DECIMAL(11, 8) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index if it doesn't exist
SET @idx_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'restaurants' 
    AND INDEX_NAME = 'idx_restaurant_location'
);

SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_restaurant_location ON restaurants(latitude, longitude)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


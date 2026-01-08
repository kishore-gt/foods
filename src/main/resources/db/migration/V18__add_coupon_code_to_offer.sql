-- Migration: Add coupon_code to offer table
-- Version: V18
-- Description: Adds coupon_code column for code-based offers

ALTER TABLE offer
ADD COLUMN coupon_code VARCHAR(50) NULL COMMENT 'Coupon code like "SAVE20", NULL for auto-apply offers',
ADD INDEX idx_coupon_code (coupon_code);

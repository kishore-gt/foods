-- Migration: Restaurant-Specific Subscription Packages
-- Version: V14
-- Description: Links subscription packages to restaurants and removes global packages

-- Delete old global packages (they should be restaurant-specific now)
DELETE FROM subscription_packages;

-- Add restaurant_id to subscription_packages
ALTER TABLE subscription_packages
    ADD COLUMN restaurant_id BIGINT NOT NULL;

-- Add foreign key constraint
ALTER TABLE subscription_packages
    ADD CONSTRAINT fk_subscription_package_restaurant 
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE;

-- Add index for restaurant_id
CREATE INDEX idx_subscription_package_restaurant_id ON subscription_packages(restaurant_id);


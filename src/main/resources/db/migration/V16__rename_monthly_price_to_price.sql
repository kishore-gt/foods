-- Migration: Rename monthly_price to price in subscription_packages
-- Version: V16
-- Description: Renames the monthly_price column to price to support daily pricing model

ALTER TABLE subscription_packages CHANGE monthly_price price DECIMAL(10, 2) NOT NULL;

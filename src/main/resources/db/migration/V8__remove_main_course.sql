-- Migration: Remove MAIN_COURSE category
-- Version: V8
-- Description: Removes MAIN_COURSE category from menu items and updates them to other categories

-- Update items with MAIN_COURSE category to STARTERS or NULL
UPDATE menu_items 
SET category = 'STARTERS' 
WHERE category = 'MAIN_COURSE';

-- Alternative: Set to NULL if you want to remove the category completely
-- UPDATE menu_items SET category = NULL WHERE category = 'MAIN_COURSE';


# Quick Fix for Failed Migration V9

## The Problem
Flyway has a failed migration record for version 9, preventing the application from starting.

## The Solution (Choose One)

### Option 1: Run the Fix Script (Easiest)
```bash
./fix-failed-migration.sh
```
Enter your MySQL password when prompted, then restart the application.

### Option 2: Manual SQL Fix
1. Connect to MySQL:
```bash
mysql -u root -p srfood
```

2. Run this SQL:
```sql
DELETE FROM flyway_schema_history WHERE version = '9';
```

3. Exit MySQL and restart your Spring Boot application.

### Option 3: If Columns/Tables Were Partially Created

If the migration partially ran, you may need to clean up:

```sql
-- Check what exists
SHOW TABLES LIKE 'restaurant%';
DESCRIBE restaurants;

-- If tables exist but are incomplete, drop them:
DROP TABLE IF EXISTS restaurant_category_tags;
DROP TABLE IF EXISTS restaurant_reviews;
DROP TABLE IF EXISTS menu_item_reviews;

-- Remove failed migration record
DELETE FROM flyway_schema_history WHERE version = '9';

-- Check for partially added columns in restaurants table
-- If columns exist, you may need to drop them manually or let migration retry
```

## After Fixing

1. Restart your Spring Boot application
2. The migration V9 will run again
3. If it still fails, check the error message and we can fix the specific issue

## Current Configuration

I've temporarily set `spring.flyway.continue-on-error=true` to help diagnose issues. Once the migration succeeds, you can set it back to `false` for production.


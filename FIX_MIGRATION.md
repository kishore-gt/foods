# Fix Failed Migration V9

The migration V9 failed partially. To fix this, you have two options:

## Option 1: Manual SQL Fix (Recommended)

1. Connect to MySQL:
```bash
mysql -u root -p tummygo
```

2. Run these commands:
```sql
-- Remove the failed migration record
DELETE FROM flyway_schema_history WHERE version = '9' AND success = 0;

-- If that doesn't work, try:
DELETE FROM flyway_schema_history WHERE version = '9';
```

3. Check if any tables/columns were partially created and drop them if needed:
```sql
-- Check if restaurant_category_tags exists
SHOW TABLES LIKE 'restaurant_category_tags';

-- Check if restaurant_reviews exists  
SHOW TABLES LIKE 'restaurant_reviews';

-- Check if menu_item_reviews exists
SHOW TABLES LIKE 'menu_item_reviews';

-- If they exist but are incomplete, drop them:
-- DROP TABLE IF EXISTS restaurant_category_tags;
-- DROP TABLE IF EXISTS restaurant_reviews;
-- DROP TABLE IF EXISTS menu_item_reviews;

-- Check if new columns were added to restaurants table
DESCRIBE restaurants;

-- If columns exist but migration failed, you may need to manually remove them
-- or let the migration retry (it will skip existing columns)
```

4. Restart the application - it should retry the migration.

## Option 2: Use the fix_failed_migration.sql script

Run the SQL script provided:
```bash
mysql -u root -p tummygo < fix_failed_migration.sql
```

Then restart the application.


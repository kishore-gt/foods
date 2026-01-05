# How to Reset Flyway Migration

The migration V2 failed because of table creation order. The file has been fixed, but Flyway won't retry a failed migration automatically.

## Option 1: Clean Flyway History (Recommended)

Run these SQL commands in MySQL to remove the failed migration record:

```sql
USE tummygo;

-- Remove the failed migration record
DELETE FROM flyway_schema_history WHERE version = '2';

-- If tables were partially created, drop them:
DROP TABLE IF EXISTS rating_review;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS restaurant_rider;
DROP TABLE IF EXISTS sub_order_item;
DROP TABLE IF EXISTS sub_order;
DROP TABLE IF EXISTS rider;
DROP TABLE IF EXISTS preorder_slot;
DROP TABLE IF EXISTS multi_order;
```

Then restart the application - Flyway will retry the migration.

## Option 2: Clean Build and Database

1. Clean the build:
   ```bash
   rm -rf target/
   ```

2. Clean Flyway history (run in MySQL):
   ```sql
   USE tummygo;
   DELETE FROM flyway_schema_history WHERE version = '2';
   ```

3. Restart the application

## Option 3: Repair Migration (If tables were partially created)

If some tables were created but not all:

1. Check which tables exist:
   ```sql
   SHOW TABLES LIKE 'multi_order';
   SHOW TABLES LIKE 'preorder_slot';
   SHOW TABLES LIKE 'rider';
   SHOW TABLES LIKE 'sub_order';
   ```

2. Drop only the tables that exist, then delete the Flyway record and restart.

## Verification

After restarting, check the logs for:
```
Successfully migrated schema `tummygo` to version "2 - multiorder suborder preorder"
```

If you see this, the migration succeeded!


# Fix Flyway Migration Failure - Step by Step

## Problem
Flyway detected a failed migration (V2) and won't allow the application to start until it's cleaned up.

## Solution

### Step 1: Run the Cleanup SQL Script

Open MySQL and run the cleanup script:

```bash
mysql -u root -p tummygo < fix_flyway_migration.sql
```

Or manually in MySQL:

```sql
USE tummygo;

-- Drop partially created tables
DROP TABLE IF EXISTS rating_review;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS restaurant_rider;
DROP TABLE IF EXISTS sub_order_item;
DROP TABLE IF EXISTS sub_order;
DROP TABLE IF EXISTS rider;
DROP TABLE IF EXISTS preorder_slot;
DROP TABLE IF EXISTS multi_order;

-- Remove failed migration record
DELETE FROM flyway_schema_history WHERE version = '2';
```

### Step 2: Clean Build (Optional but Recommended)

```bash
cd /Users/kishoremovva/Downloads/TummyGo
rm -rf target/
```

### Step 3: Restart Application

Restart your Spring Boot application. Flyway will:
1. Detect that version 2 is missing from history
2. Run the corrected migration file
3. Create all tables in the correct order

### Step 4: Verify Success

Check the logs for:
```
Successfully migrated schema `tummygo` to version "2 - multiorder suborder preorder"
```

## Alternative: Use Flyway Repair Command

If you have Flyway CLI installed:

```bash
flyway repair -url=jdbc:mysql://localhost:3306/tummygo -user=root -password=Kishore@123
```

But the SQL script above is simpler and more reliable.

## What Was Fixed

The migration file `V2__multiorder_suborder_preorder.sql` has been corrected:
- ✅ `preorder_slot` table is created BEFORE `sub_order`
- ✅ `rider` table is created BEFORE `sub_order`
- ✅ `sub_order` can now safely reference both tables
- ✅ All foreign keys are in the correct order

The file is correct - we just need to clean up the failed attempt from Flyway's history.


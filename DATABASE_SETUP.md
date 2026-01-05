# Database Setup Guide

## Problem: "Communications link failure" Error

If you see this error when starting the application, it means MySQL is not running.

## Quick Fix

### Step 1: Start MySQL Server

Run this command in your terminal (you'll be prompted for your password):

```bash
sudo /usr/local/mysql/support-files/mysql.server start
```

**Alternative methods:**
- If installed via Homebrew: `brew services start mysql`
- If MySQL is in PATH: `mysql.server start`

### Step 2: Verify MySQL is Running

```bash
lsof -i :3306
```

You should see MySQL listening on port 3306.

### Step 3: Create the Database

```bash
mysql -u root -p'Kishore@123' -e "CREATE DATABASE IF NOT EXISTS tummygo;"
```

Or if you prefer to enter the password interactively:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS tummygo;"
```

### Step 4: Verify Database Connection

```bash
mysql -u root -p'Kishore@123' tummygo -e "SELECT 'Database connected!' as status;"
```

### Step 5: Restart Your Spring Boot Application

The application should now connect successfully. Flyway will automatically run migrations on startup.

## Configuration

The application is configured to connect to:
- **Host:** localhost
- **Port:** 3306
- **Database:** tummygo
- **Username:** root
- **Password:** Kishore@123

You can change these settings in `src/main/resources/application.properties`.

## Troubleshooting

### MySQL won't start

1. Check if MySQL is already running:
   ```bash
   lsof -i :3306
   ```

2. Check MySQL error logs:
   ```bash
   tail -f /usr/local/mysql/data/*.err
   ```

3. Try stopping and restarting:
   ```bash
   sudo /usr/local/mysql/support-files/mysql.server stop
   sudo /usr/local/mysql/support-files/mysql.server start
   ```

### Wrong password

If the password in `application.properties` doesn't match your MySQL root password:

1. Update `src/main/resources/application.properties`:
   ```properties
   spring.datasource.password=your_actual_password
   ```

2. Or reset MySQL root password (if needed):
   ```bash
   sudo /usr/local/mysql/support-files/mysql.server stop
   sudo mysqld_safe --skip-grant-tables &
   mysql -u root
   # Then run: ALTER USER 'root'@'localhost' IDENTIFIED BY 'Kishore@123';
   ```

### Database doesn't exist

Create it manually:
```bash
mysql -u root -p -e "CREATE DATABASE tummygo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

## Auto-start MySQL on Boot (Optional)

To start MySQL automatically when your Mac boots:

```bash
sudo /usr/local/mysql/support-files/mysql.server install
```

Or if using Homebrew:
```bash
brew services start mysql
```

## Using the Helper Script

You can also use the provided helper script:

```bash
./start-mysql.sh
```

Note: This script will prompt for your sudo password to start MySQL.


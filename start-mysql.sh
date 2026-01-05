#!/bin/bash

# Script to help start MySQL on macOS

echo "=== MySQL Startup Helper ==="
echo ""

# Check if MySQL is running
if lsof -i :3306 > /dev/null 2>&1; then
    echo "✅ MySQL is already running on port 3306"
    echo ""
    echo "Verifying database exists..."
    mysql -u root -p'Kishore@123' -e "USE srfood;" 2>/dev/null && echo "✅ Database 'srfood' exists" || echo "⚠️  Database 'srfood' may not exist"
    exit 0
fi

echo "❌ MySQL is not running on port 3306"
echo ""

# Try to find MySQL installation
MYSQL_SERVER=""
if [ -f "/usr/local/mysql/support-files/mysql.server" ]; then
    MYSQL_SERVER="/usr/local/mysql/support-files/mysql.server"
elif [ -f "/opt/homebrew/opt/mysql/support-files/mysql.server" ]; then
    MYSQL_SERVER="/opt/homebrew/opt/mysql/support-files/mysql.server"
else
    # Try to find it
    MYSQL_SERVER=$(find /usr/local -name mysql.server 2>/dev/null | head -1)
fi

if [ -n "$MYSQL_SERVER" ] && [ -f "$MYSQL_SERVER" ]; then
    echo "Found MySQL at: $MYSQL_SERVER"
    echo ""
    echo "Attempting to start MySQL (may require password)..."
    sudo "$MYSQL_SERVER" start
    
    # Wait a moment for MySQL to start
    sleep 2
    
    # Check if it started
    if lsof -i :3306 > /dev/null 2>&1; then
        echo "✅ MySQL started successfully!"
        echo ""
        echo "Creating database if it doesn't exist..."
        mysql -u root -p'Kishore@123' -e "CREATE DATABASE IF NOT EXISTS srfood;" 2>/dev/null && echo "✅ Database 'srfood' is ready" || echo "⚠️  Could not create database (check password)"
    else
        echo "❌ Failed to start MySQL. Please start it manually:"
        echo "   sudo $MYSQL_SERVER start"
    fi
else
    echo "MySQL server script not found automatically."
    echo ""
    echo "To start MySQL manually, try one of these methods:"
    echo ""
    echo "1. If installed via Homebrew:"
    echo "   brew services start mysql"
    echo "   OR"
    echo "   mysql.server start"
    echo ""
    echo "2. If installed via MySQL Installer:"
    echo "   sudo /usr/local/mysql/support-files/mysql.server start"
    echo ""
    echo "3. After starting MySQL, create the database:"
    echo "   mysql -u root -p -e 'CREATE DATABASE IF NOT EXISTS srfood;'"
    echo ""
    echo "4. Verify MySQL is running:"
    echo "   lsof -i :3306"
fi

echo ""


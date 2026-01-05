-- Migration: Company Subscription System
-- Version: V12
-- Description: Adds tables for company registration, subscription packages, and company orders

-- Company table: Company profile information
CREATE TABLE IF NOT EXISTS companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    company_name VARCHAR(200) NOT NULL,
    company_address VARCHAR(500),
    office_phone VARCHAR(20),
    contact_person_name VARCHAR(100),
    number_of_employees INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Subscription Package table: Different food packages with pricing
CREATE TABLE IF NOT EXISTS subscription_packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    number_of_people INT NOT NULL, -- 10 or 20
    monthly_price DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_number_of_people (number_of_people),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Company Subscription table: Active subscriptions for companies
CREATE TABLE IF NOT EXISTS company_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    subscription_package_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, CANCELLED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_company_id (company_id),
    INDEX idx_subscription_package_id (subscription_package_id),
    INDEX idx_status (status),
    INDEX idx_dates (start_date, end_date),
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (subscription_package_id) REFERENCES subscription_packages(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Company Order table: Food orders placed by companies
CREATE TABLE IF NOT EXISTS company_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    order_date DATE NOT NULL,
    quantity INT NOT NULL, -- Number of servings (10 or 20)
    total_amount DECIMAL(10, 2) NOT NULL,
    special_instructions VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, PREPARING, DELIVERED, CANCELLED
    delivery_time TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_company_id (company_id),
    INDEX idx_menu_item_id (menu_item_id),
    INDEX idx_order_date (order_date),
    INDEX idx_status (status),
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default subscription packages
INSERT INTO subscription_packages (name, description, number_of_people, monthly_price, is_active) VALUES
('Basic Package - 10 People', 'Monthly subscription for 10 people. Perfect for small teams.', 10, 5000.00, TRUE),
('Premium Package - 10 People', 'Monthly subscription for 10 people with premium options.', 10, 7500.00, TRUE),
('Basic Package - 20 People', 'Monthly subscription for 20 people. Great for medium-sized offices.', 20, 9000.00, TRUE),
('Premium Package - 20 People', 'Monthly subscription for 20 people with premium options.', 20, 13000.00, TRUE);


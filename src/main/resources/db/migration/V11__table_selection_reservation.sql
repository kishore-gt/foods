-- Migration: Table Selection and Reservation System
-- Version: V11
-- Description: Adds tables for restaurant table management, reservations, and enhanced preorder system with table selection

-- Restaurant Table table: Physical tables in restaurants
CREATE TABLE IF NOT EXISTS restaurant_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    table_number VARCHAR(20) NOT NULL,
    table_name VARCHAR(100),
    capacity INT NOT NULL DEFAULT 2,
    table_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD', -- STANDARD, FAMILY, VIP, OUTDOOR, etc.
    floor_number INT DEFAULT 1,
    section_name VARCHAR(50), -- e.g., "Garden", "Fountain", "1st floor"
    x_position INT, -- For floor plan visualization
    y_position INT, -- For floor plan visualization
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_restaurant_id (restaurant_id),
    INDEX idx_table_type (table_type),
    INDEX idx_section (section_name),
    INDEX idx_is_active (is_active),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    UNIQUE KEY unique_restaurant_table (restaurant_id, table_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table Reservation table: Reservations for tables
CREATE TABLE IF NOT EXISTS table_reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reservation_date DATE NOT NULL,
    reservation_time TIME NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 60, -- Duration in minutes
    number_of_guests INT NOT NULL DEFAULT 2,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW
    check_in_time TIMESTAMP NULL,
    check_out_time TIMESTAMP NULL,
    special_requests VARCHAR(500),
    qr_code VARCHAR(100) UNIQUE, -- QR code for check-in
    auto_release_time TIMESTAMP, -- Auto-release if not checked in
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_restaurant_id (restaurant_id),
    INDEX idx_table_id (table_id),
    INDEX idx_user_id (user_id),
    INDEX idx_reservation_datetime (reservation_date, reservation_time),
    INDEX idx_status (status),
    INDEX idx_qr_code (qr_code),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    FOREIGN KEY (table_id) REFERENCES restaurant_table(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add table_id and reservation_id to sub_order for linking orders to tables
-- Note: Flyway ensures this migration runs only once, so we can safely add columns
ALTER TABLE sub_order 
    ADD COLUMN table_id BIGINT NULL,
    ADD COLUMN reservation_id BIGINT NULL,
    ADD COLUMN order_type VARCHAR(30) DEFAULT 'DELIVERY',
    ADD COLUMN preparation_start_time TIMESTAMP NULL;

-- Add indexes for sub_order new columns
CREATE INDEX idx_sub_order_table_id ON sub_order(table_id);
CREATE INDEX idx_sub_order_reservation_id ON sub_order(reservation_id);
CREATE INDEX idx_sub_order_order_type ON sub_order(order_type);

-- Add foreign keys for sub_order (after restaurant_table and table_reservation are created)
ALTER TABLE sub_order 
    ADD CONSTRAINT fk_sub_order_table FOREIGN KEY (table_id) REFERENCES restaurant_table(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_sub_order_reservation FOREIGN KEY (reservation_id) REFERENCES table_reservation(id) ON DELETE SET NULL;

-- Add payment_method to multi_order for pay now vs pay at restaurant
ALTER TABLE multi_order
    ADD COLUMN payment_method VARCHAR(50) DEFAULT 'ONLINE',
    ADD COLUMN payment_at_restaurant BOOLEAN DEFAULT FALSE;

-- Table Status History: Track table status changes over time
CREATE TABLE IF NOT EXISTS table_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id BIGINT NOT NULL,
    reservation_id BIGINT NULL,
    status VARCHAR(30) NOT NULL, -- FREE, RESERVED, OCCUPIED, CLEANING
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by_user_id BIGINT NULL,
    notes VARCHAR(500),
    INDEX idx_table_id (table_id),
    INDEX idx_reservation_id (reservation_id),
    INDEX idx_changed_at (changed_at),
    FOREIGN KEY (table_id) REFERENCES restaurant_table(id) ON DELETE CASCADE,
    FOREIGN KEY (reservation_id) REFERENCES table_reservation(id) ON DELETE SET NULL,
    FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


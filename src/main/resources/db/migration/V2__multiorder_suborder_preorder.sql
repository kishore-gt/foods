-- Migration: MultiOrder, SubOrder, PreorderSlot, Rider, Payment, Notification, RatingReview
-- Version: V2
-- Description: Adds tables for multi-restaurant multi-order architecture with preorder slots,
--              rider assignment, payments, notifications, and ratings

-- MultiOrder table: Represents a customer order that can span multiple restaurants
CREATE TABLE IF NOT EXISTS multi_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    applied_coupon VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    delivery_address VARCHAR(200) NOT NULL,
    special_instructions VARCHAR(500),
    payment_status VARCHAR(30) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_payment_status (payment_status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PreorderSlot table: Time slots for preorders with atomic capacity management
-- Must be created before sub_order since sub_order references it
CREATE TABLE IF NOT EXISTS preorder_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    slot_start_time TIMESTAMP NOT NULL,
    slot_end_time TIMESTAMP NOT NULL,
    max_capacity INT NOT NULL DEFAULT 10,
    current_capacity INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_restaurant_id (restaurant_id),
    INDEX idx_slot_times (slot_start_time, slot_end_time),
    INDEX idx_is_active (is_active),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    UNIQUE KEY unique_restaurant_slot (restaurant_id, slot_start_time, slot_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rider table: Delivery personnel
-- Must be created before sub_order since sub_order references it
CREATE TABLE IF NOT EXISTS rider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    current_latitude DECIMAL(10, 8),
    current_longitude DECIMAL(11, 8),
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) DEFAULT 'IDLE',
    vehicle_type VARCHAR(50),
    vehicle_number VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_is_online (is_online),
    INDEX idx_is_available (is_available),
    INDEX idx_location (current_latitude, current_longitude),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- SubOrder table: One per restaurant in a MultiOrder
-- Must be created after preorder_slot and rider tables
CREATE TABLE IF NOT EXISTS sub_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    multi_order_id BIGINT NOT NULL,
    restaurant_id BIGINT,
    chef_profile_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    rider_id BIGINT,
    preorder_slot_id BIGINT,
    estimated_delivery_time TIMESTAMP,
    actual_delivery_time TIMESTAMP,
    tracking_info VARCHAR(500),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_multi_order_id (multi_order_id),
    INDEX idx_restaurant_id (restaurant_id),
    INDEX idx_chef_profile_id (chef_profile_id),
    INDEX idx_rider_id (rider_id),
    INDEX idx_status (status),
    INDEX idx_preorder_slot_id (preorder_slot_id),
    FOREIGN KEY (multi_order_id) REFERENCES multi_order(id) ON DELETE CASCADE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE SET NULL,
    FOREIGN KEY (chef_profile_id) REFERENCES chef_profiles(id) ON DELETE SET NULL,
    FOREIGN KEY (rider_id) REFERENCES rider(id) ON DELETE SET NULL,
    FOREIGN KEY (preorder_slot_id) REFERENCES preorder_slot(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- SubOrderItem table: Individual items in a SubOrder
CREATE TABLE IF NOT EXISTS sub_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sub_order_id BIGINT NOT NULL,
    menu_item_id BIGINT,
    item_name VARCHAR(200) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    line_total DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sub_order_id (sub_order_id),
    INDEX idx_menu_item_id (menu_item_id),
    FOREIGN KEY (sub_order_id) REFERENCES sub_order(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- RestaurantRider table: Many-to-many relationship between restaurants and riders
CREATE TABLE IF NOT EXISTS restaurant_rider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    rider_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_restaurant_id (restaurant_id),
    INDEX idx_rider_id (rider_id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    FOREIGN KEY (rider_id) REFERENCES rider(id) ON DELETE CASCADE,
    UNIQUE KEY unique_restaurant_rider (restaurant_id, rider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Payment table: Payment records for orders
CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    multi_order_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(100),
    otp_required BOOLEAN NOT NULL DEFAULT FALSE,
    otp_code VARCHAR(6),
    otp_verified BOOLEAN NOT NULL DEFAULT FALSE,
    otp_expires_at TIMESTAMP,
    payment_gateway_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_multi_order_id (multi_order_id),
    INDEX idx_payment_status (payment_status),
    INDEX idx_transaction_id (transaction_id),
    FOREIGN KEY (multi_order_id) REFERENCES multi_order(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notification table: System notifications for users
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    INDEX idx_related_entity (related_entity_type, related_entity_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- RatingReview table: Enhanced rating and review system
CREATE TABLE IF NOT EXISTS rating_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT,
    chef_profile_id BIGINT,
    sub_order_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_restaurant_id (restaurant_id),
    INDEX idx_chef_profile_id (chef_profile_id),
    INDEX idx_sub_order_id (sub_order_id),
    INDEX idx_rating (rating),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    FOREIGN KEY (chef_profile_id) REFERENCES chef_profiles(id) ON DELETE CASCADE,
    FOREIGN KEY (sub_order_id) REFERENCES sub_order(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_suborder_review (user_id, sub_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


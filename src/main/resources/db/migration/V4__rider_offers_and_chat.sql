-- Migration: Rider Offers and Chat System
-- Version: V4
-- Description: Adds rider offer system (approve/reject before assignment) and chat support

-- RiderOffer table: Tracks pending offers to riders before assignment
CREATE TABLE IF NOT EXISTS rider_offer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sub_order_id BIGINT NOT NULL,
    rider_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, REJECTED, EXPIRED
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sub_order_id (sub_order_id),
    INDEX idx_rider_id (rider_id),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at),
    FOREIGN KEY (sub_order_id) REFERENCES sub_order(id) ON DELETE CASCADE,
    FOREIGN KEY (rider_id) REFERENCES rider(id) ON DELETE CASCADE,
    UNIQUE KEY unique_active_offer (sub_order_id, rider_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ChatMessage table: Stores chat messages between users, riders, and restaurants
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL, -- User ID (can be customer, rider, or restaurant owner)
    receiver_id BIGINT NOT NULL, -- User ID
    order_id BIGINT, -- Optional: link to MultiOrder for order-specific chats
    sub_order_id BIGINT, -- Optional: link to SubOrder for sub-order-specific chats
    message TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT', -- TEXT, IMAGE, SYSTEM
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_order_id (order_id),
    INDEX idx_sub_order_id (sub_order_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES multi_order(id) ON DELETE CASCADE,
    FOREIGN KEY (sub_order_id) REFERENCES sub_order(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ChatRoom table: Represents a chat conversation between participants
CREATE TABLE IF NOT EXISTS chat_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT, -- Optional: link to MultiOrder
    sub_order_id BIGINT, -- Optional: link to SubOrder
    participant1_id BIGINT NOT NULL, -- User ID
    participant2_id BIGINT NOT NULL, -- User ID
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_sub_order_id (sub_order_id),
    INDEX idx_participants (participant1_id, participant2_id),
    INDEX idx_last_message_at (last_message_at),
    FOREIGN KEY (order_id) REFERENCES multi_order(id) ON DELETE CASCADE,
    FOREIGN KEY (sub_order_id) REFERENCES sub_order(id) ON DELETE CASCADE,
    FOREIGN KEY (participant1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (participant2_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_chat_room (participant1_id, participant2_id, sub_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add OFFERED status support to sub_order (if not already present)
-- Note: This is handled by application logic, but we ensure the column can handle it
-- The status column already exists and is VARCHAR(30), so it can handle 'OFFERED'


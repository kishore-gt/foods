-- Migration: Package Menu Items
-- Version: V15
-- Description: Links subscription packages to menu items (food items in packages)

-- Create package_menu_items table to link subscription packages with menu items
CREATE TABLE IF NOT EXISTS package_menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_package_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    INDEX idx_subscription_package_id (subscription_package_id),
    INDEX idx_menu_item_id (menu_item_id),
    FOREIGN KEY (subscription_package_id) REFERENCES subscription_packages(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE,
    UNIQUE KEY unique_package_menu_item (subscription_package_id, menu_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

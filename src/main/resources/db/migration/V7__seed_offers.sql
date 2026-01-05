-- Migration: Seed Sample Offers
-- Version: V7
-- Description: Adds sample offers for restaurants

-- Insert sample offers
INSERT INTO offer (restaurant_id, title, description, offer_type, discount_value, min_order_amount, max_discount, start_date, end_date, is_active, image_url) VALUES
(1, '20% OFF', 'Get 20% off on all orders above ₹500', 'PERCENTAGE_OFF', 20.00, 500.00, 200.00, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), TRUE, 'https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=800'),
(2, 'Buy 1 Get 1 Free', 'Buy any item and get another one free!', 'BUY_ONE_GET_ONE', NULL, 300.00, NULL, NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), TRUE, 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=800'),
(3, 'Flat ₹100 OFF', 'Flat discount of ₹100 on orders above ₹400', 'FLAT_DISCOUNT', 100.00, 400.00, 100.00, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), TRUE, 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800'),
(4, '30% OFF', 'Enjoy 30% off on all desserts', 'PERCENTAGE_OFF', 30.00, 200.00, 150.00, NOW(), DATE_ADD(NOW(), INTERVAL 25 DAY), TRUE, 'https://images.unsplash.com/photo-1551024506-0bccd828d307?w=800'),
(5, 'Free Delivery', 'Free delivery on orders above ₹250', 'FREE_DELIVERY', NULL, 250.00, NULL, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), TRUE, 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800'),
(6, 'Combo Offer', 'Special combo meal at discounted price', 'COMBO_OFFER', 15.00, 350.00, 100.00, NOW(), DATE_ADD(NOW(), INTERVAL 18 DAY), TRUE, 'https://images.unsplash.com/photo-1551782450-a2132b4ba21d?w=800'),
(7, '25% OFF', 'Get 25% off on all starters', 'PERCENTAGE_OFF', 25.00, 300.00, 120.00, NOW(), DATE_ADD(NOW(), INTERVAL 22 DAY), TRUE, 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=800'),
(8, 'Buy 2 Get 1 Free', 'Buy 2 pizzas and get 1 free', 'BUY_ONE_GET_ONE', NULL, 500.00, NULL, NOW(), DATE_ADD(NOW(), INTERVAL 28 DAY), TRUE, 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800'),
(9, 'Flat ₹150 OFF', 'Flat discount of ₹150 on orders above ₹600', 'FLAT_DISCOUNT', 150.00, 600.00, 150.00, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), TRUE, 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800'),
(10, '40% OFF', 'Massive 40% off on all beverages', 'PERCENTAGE_OFF', 40.00, 150.00, 80.00, NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), TRUE, 'https://images.unsplash.com/photo-1544145945-f90425340c7e?w=800');

-- Update menu items with categories and veg/non-veg status
-- This is a sample update - in production, you'd want to properly categorize each item
UPDATE menu_items SET category = 'STARTERS', is_veg = TRUE WHERE category IS NULL AND id % 3 = 0;
UPDATE menu_items SET category = 'STARTERS', is_veg = TRUE WHERE category IS NULL AND id % 3 = 1;
UPDATE menu_items SET category = 'DESSERTS', is_veg = TRUE WHERE category IS NULL AND id % 3 = 2;
UPDATE menu_items SET category = 'BEVERAGES', is_veg = TRUE WHERE category IS NULL AND id % 5 = 0;
UPDATE menu_items SET category = 'SNACKS', is_veg = FALSE WHERE category IS NULL AND id % 7 = 0;


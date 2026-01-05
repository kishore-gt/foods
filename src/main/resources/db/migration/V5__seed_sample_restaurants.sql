-- Seed 10 sample restaurants with a default owner
-- This migration creates a default restaurant owner and 10 sample restaurants

-- Create a default restaurant owner if it doesn't exist
-- Password hash is for "password123" (BCrypt)
INSERT IGNORE INTO users (id, full_name, email, password_hash, role, phone_number, created_at, updated_at)
VALUES (1000, 'Default Restaurant Owner', 'owner@restaurant.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'OWNER', '1234567890', NOW(), NOW());

-- Insert 10 sample restaurants near a central location
-- Using coordinates around a central point (adjust these to your actual location)
-- Base coordinates: approximately 37.7749° N, -122.4194° W (San Francisco area - adjust as needed)

INSERT IGNORE INTO restaurants (name, description, address, contact_number, owner_id, latitude, longitude, opening_time, closing_time, image_url, created_at, updated_at)
VALUES
    ('Spice Garden', 'Authentic Indian cuisine with a modern twist. Specializing in North and South Indian dishes.', '123 Main Street, Downtown', '555-0101', 1000, 37.7750, -122.4190, '10:00', '22:00', 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=500', NOW(), NOW()),
    
    ('Bella Italia', 'Traditional Italian restaurant serving fresh pasta, pizza, and classic Italian dishes.', '456 Oak Avenue, Midtown', '555-0102', 1000, 37.7755, -122.4195, '11:00', '23:00', 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=500', NOW(), NOW()),
    
    ('Dragon Palace', 'Serving authentic Chinese cuisine including Szechuan, Cantonese, and Hunan specialties.', '789 Pine Street, Chinatown', '555-0103', 1000, 37.7760, -122.4200, '11:30', '22:30', 'https://images.unsplash.com/photo-1559339352-11d035aa65de?w=500', NOW(), NOW()),
    
    ('Burger Junction', 'Gourmet burgers, crispy fries, and milkshakes. Made fresh daily with premium ingredients.', '321 Elm Street, Food Court', '555-0104', 1000, 37.7745, -122.4185, '10:30', '23:30', 'https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=500', NOW(), NOW()),
    
    ('Sushi Zen', 'Fresh sushi and sashimi, traditional Japanese dishes, and premium sake selection.', '654 Maple Drive, Waterfront', '555-0105', 1000, 37.7758, -122.4198, '12:00', '22:00', 'https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?w=500', NOW(), NOW()),
    
    ('Taco Fiesta', 'Authentic Mexican street food, tacos, burritos, and fresh guacamole. Spicy and flavorful!', '987 Broadway, Market Square', '555-0106', 1000, 37.7742, -122.4180, '11:00', '23:00', 'https://images.unsplash.com/photo-1565299585323-38174c3c0b0e?w=500', NOW(), NOW()),
    
    ('Mediterranean Delight', 'Fresh Mediterranean cuisine featuring hummus, falafel, kebabs, and Greek salads.', '147 Cedar Lane, Plaza District', '555-0107', 1000, 37.7765, -122.4205, '11:30', '22:30', 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500', NOW(), NOW()),
    
    ('BBQ Smokehouse', 'Slow-smoked meats, ribs, brisket, and classic American BBQ sides. Finger-licking good!', '258 Hickory Road, Industrial Area', '555-0108', 1000, 37.7738, -122.4175, '12:00', '23:00', 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500', NOW(), NOW()),
    
    ('Thai Orchid', 'Authentic Thai cuisine with bold flavors. Pad Thai, curries, and traditional Thai dishes.', '369 Willow Way, Cultural District', '555-0109', 1000, 37.7770, -122.4210, '11:00', '22:00', 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=500', NOW(), NOW()),
    
    ('Pizza Corner', 'Wood-fired pizzas, fresh salads, and Italian appetizers. Family-friendly atmosphere.', '741 Cherry Street, Residential Area', '555-0110', 1000, 37.7735, -122.4170, '10:00', '23:30', 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500', NOW(), NOW());


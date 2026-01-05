-- Remove seeded restaurants that were added in V5
-- This migration deletes the 10 sample restaurants and the default owner
-- Uses stored procedure to handle missing tables gracefully

DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS remove_seeded_restaurants()
BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
    
    -- Delete menu items (if table exists)
    DELETE mi FROM menu_items mi 
    INNER JOIN menus m ON mi.menu_id = m.id 
    INNER JOIN restaurants r ON m.restaurant_id = r.id 
    WHERE r.owner_id = 1000;
    
    -- Delete menus (if table exists)
    DELETE m FROM menus m 
    INNER JOIN restaurants r ON m.restaurant_id = r.id 
    WHERE r.owner_id = 1000;
    
    -- Delete offers (if table exists - will be silently ignored if table doesn't exist)
    DELETE o FROM offers o 
    INNER JOIN restaurants r ON o.restaurant_id = r.id 
    WHERE r.owner_id = 1000;
    
    -- Delete restaurant category tags (if table exists)
    DELETE rct FROM restaurant_category_tags rct 
    INNER JOIN restaurants r ON rct.restaurant_id = r.id 
    WHERE r.owner_id = 1000;
    
    -- Delete restaurant reviews (if table exists)
    DELETE rr FROM restaurant_reviews rr 
    INNER JOIN restaurants r ON rr.restaurant_id = r.id 
    WHERE r.owner_id = 1000;
    
    -- Delete the restaurants
    DELETE FROM restaurants WHERE owner_id = 1000;
    
    -- Delete the owner
    DELETE FROM users WHERE id = 1000 AND role = 'OWNER';
END$$

DELIMITER ;

-- Execute the procedure
CALL remove_seeded_restaurants();

-- Drop the procedure
DROP PROCEDURE IF EXISTS remove_seeded_restaurants;


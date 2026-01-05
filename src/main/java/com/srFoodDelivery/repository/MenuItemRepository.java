package com.srFoodDelivery.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.Menu;
import com.srFoodDelivery.model.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByMenu(Menu menu);

    List<MenuItem> findByAvailableTrue();

    List<MenuItem> findByAvailableTrueAndTagsIn(Collection<String> tags);
    
    List<MenuItem> findByAvailableTrueAndCategory(String category);
    
    List<MenuItem> findByAvailableTrueAndIsVeg(boolean isVeg);
    
    List<MenuItem> findByAvailableTrueAndCategoryAndIsVeg(String category, boolean isVeg);
    
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m.category FROM MenuItem m WHERE m.category IS NOT NULL AND m.available = true")
    List<String> findDistinctCategories();
    
    @org.springframework.data.jpa.repository.Query("SELECT mi FROM MenuItem mi WHERE mi.menu.restaurant.isCafeLounge = true")
    List<MenuItem> findByCafeRestaurants();
    
    List<MenuItem> findByMenu_Restaurant_IdAndAvailableTrue(Long restaurantId);
}

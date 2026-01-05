package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.MenuItemReview;

@Repository
public interface MenuItemReviewRepository extends JpaRepository<MenuItemReview, Long> {
    
    List<MenuItemReview> findByMenuItemOrderByCreatedAtDesc(MenuItem menuItem);
    
    @Query("SELECT AVG(r.rating) FROM MenuItemReview r WHERE r.menuItem = :menuItem")
    Double getAverageRatingByMenuItem(@Param("menuItem") MenuItem menuItem);
    
    @Query("SELECT COUNT(r) FROM MenuItemReview r WHERE r.menuItem = :menuItem")
    Long countByMenuItem(@Param("menuItem") MenuItem menuItem);
}


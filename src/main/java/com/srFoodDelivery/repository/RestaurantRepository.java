package com.srFoodDelivery.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.User;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByOwner(User owner);
    
    List<Restaurant> findByIsActiveTrue();
    
    List<Restaurant> findByCuisineType(String cuisineType);
    
    List<Restaurant> findByIsActiveTrueAndCuisineType(String cuisineType);
    
    List<Restaurant> findByIsActiveTrueAndIsPureVegTrue();
    
    List<Restaurant> findByIsActiveTrueAndIsCloudKitchenTrue();
    
    List<Restaurant> findByIsActiveTrueAndIsFamilyRestaurantTrue();
    
    List<Restaurant> findByIsActiveTrueAndIsCafeLoungeTrue();
    
    @Query("SELECT DISTINCT r FROM Restaurant r JOIN r.categoryTags ct WHERE r.isActive = true AND ct IN :tags")
    List<Restaurant> findByCategoryTagsIn(@Param("tags") Set<String> tags);
    
    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true AND r.averageRating >= :minRating ORDER BY r.averageRating DESC")
    List<Restaurant> findByMinRating(@Param("minRating") BigDecimal minRating);
    
    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true AND r.deliveryTimeMinutes <= :maxMinutes ORDER BY r.deliveryTimeMinutes ASC")
    List<Restaurant> findByMaxDeliveryTime(@Param("maxMinutes") Integer maxMinutes);
    
    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true ORDER BY r.averageRating DESC, r.totalRatings DESC")
    List<Restaurant> findTopRated();
    
    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true ORDER BY r.createdAt DESC")
    List<Restaurant> findNewlyOpened();
    
    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true AND r.minOrderAmount <= :maxAmount ORDER BY r.minOrderAmount ASC")
    List<Restaurant> findByBudgetFriendly(@Param("maxAmount") BigDecimal maxAmount);
    
    List<Restaurant> findByNameIgnoreCase(String name);
}

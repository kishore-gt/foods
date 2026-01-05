package com.srFoodDelivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.RestaurantTable;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    
    List<RestaurantTable> findByRestaurantAndIsActiveTrue(Restaurant restaurant);
    
    List<RestaurantTable> findByRestaurantAndTableTypeAndIsActiveTrue(Restaurant restaurant, String tableType);
    
    List<RestaurantTable> findByRestaurantAndSectionNameAndIsActiveTrue(Restaurant restaurant, String sectionName);
    
    Optional<RestaurantTable> findByRestaurantAndTableNumber(Restaurant restaurant, String tableNumber);
    
    @Query("SELECT t FROM RestaurantTable t WHERE t.restaurant = :restaurant AND t.isActive = true ORDER BY t.floorNumber, t.sectionName, t.tableNumber")
    List<RestaurantTable> findActiveTablesByRestaurantOrdered(@Param("restaurant") Restaurant restaurant);
}


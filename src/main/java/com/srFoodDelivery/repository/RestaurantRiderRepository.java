package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.RestaurantRider;
import com.srFoodDelivery.model.Rider;

@Repository
public interface RestaurantRiderRepository extends JpaRepository<RestaurantRider, Long> {
    
    List<RestaurantRider> findByRestaurantAndIsActiveTrue(Restaurant restaurant);
    
    List<RestaurantRider> findByRiderAndIsActiveTrue(Rider rider);
    
    boolean existsByRestaurantAndRiderAndIsActiveTrue(Restaurant restaurant, Rider rider);
}


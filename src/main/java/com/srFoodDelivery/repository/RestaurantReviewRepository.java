package com.srFoodDelivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.RestaurantReview;
import com.srFoodDelivery.model.User;

@Repository
public interface RestaurantReviewRepository extends JpaRepository<RestaurantReview, Long> {
    
    List<RestaurantReview> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);
    
    Optional<RestaurantReview> findByUserAndRestaurant(User user, Restaurant restaurant);
    
    Optional<RestaurantReview> findByUserAndRestaurantAndOrderId(User user, Restaurant restaurant, Long orderId);
    
    @Query("SELECT AVG(r.rating) FROM RestaurantReview r WHERE r.restaurant = :restaurant")
    Double getAverageRatingByRestaurant(@Param("restaurant") Restaurant restaurant);
    
    @Query("SELECT COUNT(r) FROM RestaurantReview r WHERE r.restaurant = :restaurant")
    Long countByRestaurant(@Param("restaurant") Restaurant restaurant);
}


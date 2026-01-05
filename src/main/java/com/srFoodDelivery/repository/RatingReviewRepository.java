package com.srFoodDelivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.ChefProfile;
import com.srFoodDelivery.model.RatingReview;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;

@Repository
public interface RatingReviewRepository extends JpaRepository<RatingReview, Long> {
    
    List<RatingReview> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);
    
    List<RatingReview> findByChefProfileOrderByCreatedAtDesc(ChefProfile chefProfile);
    
    Optional<RatingReview> findByUserAndSubOrder(User user, SubOrder subOrder);
    
    @Query("SELECT AVG(rr.rating) FROM RatingReview rr WHERE rr.restaurant = :restaurant")
    Double getAverageRatingByRestaurant(@Param("restaurant") Restaurant restaurant);
    
    @Query("SELECT AVG(rr.rating) FROM RatingReview rr WHERE rr.chefProfile = :chefProfile")
    Double getAverageRatingByChefProfile(@Param("chefProfile") ChefProfile chefProfile);
    
    @Query("SELECT COUNT(rr) FROM RatingReview rr WHERE rr.restaurant = :restaurant")
    Long countByRestaurant(@Param("restaurant") Restaurant restaurant);
    
    @Query("SELECT COUNT(rr) FROM RatingReview rr WHERE rr.chefProfile = :chefProfile")
    Long countByChefProfile(@Param("chefProfile") ChefProfile chefProfile);
}


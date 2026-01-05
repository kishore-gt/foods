package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.MenuItem;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.Review;
import com.srFoodDelivery.model.User;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);
    List<Review> findByMenuItemOrderByCreatedAtDesc(MenuItem menuItem);
    List<Review> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndRestaurant(User user, Restaurant restaurant);
    boolean existsByUserAndMenuItem(User user, MenuItem menuItem);
}


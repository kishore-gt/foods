package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.ChefProfile;
import com.srFoodDelivery.model.Order;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByCreatedAtDesc(User user);
    List<Order> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);
    List<Order> findByChefProfileOrderByCreatedAtDesc(ChefProfile chefProfile);
    List<Order> findAllByOrderByCreatedAtDesc();
    long countByUser(User user);
}

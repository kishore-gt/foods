package com.srFoodDelivery.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.Cart;
import com.srFoodDelivery.model.User;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);

    void deleteByUser(User user);
}

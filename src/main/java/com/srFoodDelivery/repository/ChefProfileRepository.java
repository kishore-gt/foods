package com.srFoodDelivery.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.ChefProfile;
import com.srFoodDelivery.model.User;

public interface ChefProfileRepository extends JpaRepository<ChefProfile, Long> {
    Optional<ChefProfile> findByChef(User chef);
}

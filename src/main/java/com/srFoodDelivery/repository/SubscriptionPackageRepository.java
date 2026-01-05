package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.SubscriptionPackage;

public interface SubscriptionPackageRepository extends JpaRepository<SubscriptionPackage, Long> {
    List<SubscriptionPackage> findByIsActiveTrue();
    List<SubscriptionPackage> findByRestaurantAndIsActiveTrue(Restaurant restaurant);
    List<SubscriptionPackage> findByRestaurantAndNumberOfPeopleAndIsActiveTrue(Restaurant restaurant, Integer numberOfPeople);
    List<SubscriptionPackage> findByRestaurantIdAndIsActiveTrue(Long restaurantId);
}


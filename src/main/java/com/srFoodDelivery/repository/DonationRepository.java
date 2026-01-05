package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.Donation;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.User;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);
    List<Donation> findByIsClaimedFalseOrderByCreatedAtDesc();
    List<Donation> findByClaimedByOrderByClaimedAtDesc(User user);
}


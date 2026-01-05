package com.srFoodDelivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.DeliveryPerson;
import com.srFoodDelivery.model.User;

public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, Long> {
    Optional<DeliveryPerson> findByUser(User user);
    List<DeliveryPerson> findByIsAvailableTrue();
}


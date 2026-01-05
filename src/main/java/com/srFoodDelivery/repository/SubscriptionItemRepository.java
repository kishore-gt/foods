package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.CompanySubscription;
import com.srFoodDelivery.model.SubscriptionItem;

public interface SubscriptionItemRepository extends JpaRepository<SubscriptionItem, Long> {
    List<SubscriptionItem> findBySubscription(CompanySubscription subscription);
    void deleteBySubscription(CompanySubscription subscription);
}


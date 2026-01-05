package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.PackageMenuItem;
import com.srFoodDelivery.model.SubscriptionPackage;

public interface PackageMenuItemRepository extends JpaRepository<PackageMenuItem, Long> {
    List<PackageMenuItem> findBySubscriptionPackage(SubscriptionPackage subscriptionPackage);
    void deleteBySubscriptionPackage(SubscriptionPackage subscriptionPackage);
    boolean existsBySubscriptionPackageAndMenuItemId(SubscriptionPackage subscriptionPackage, Long menuItemId);
}

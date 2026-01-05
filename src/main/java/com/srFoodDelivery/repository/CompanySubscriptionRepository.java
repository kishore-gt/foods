package com.srFoodDelivery.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.Company;
import com.srFoodDelivery.model.CompanySubscription;
import com.srFoodDelivery.model.SubscriptionPackage;

public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Long> {
    List<CompanySubscription> findByCompany(Company company);
    Optional<CompanySubscription> findByCompanyAndStatus(Company company, String status);
    List<CompanySubscription> findByCompanyAndStatusOrderByStartDateDesc(Company company, String status);
    List<CompanySubscription> findByStatusAndEndDateAfter(String status, LocalDate date);

    List<CompanySubscription> findBySubscriptionPackage(SubscriptionPackage subscriptionPackage);

    List<CompanySubscription> findBySubscriptionPackage_Restaurant_IdAndStatusOrderByCreatedAtDesc(Long restaurantId,
            String status);
}


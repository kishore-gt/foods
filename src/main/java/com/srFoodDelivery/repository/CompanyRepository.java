package com.srFoodDelivery.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.Company;
import com.srFoodDelivery.model.User;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByUser(User user);
    Optional<Company> findByUserId(Long userId);
    boolean existsByUser(User user);
}


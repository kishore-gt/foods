package com.srFoodDelivery.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.srFoodDelivery.model.Company;
import com.srFoodDelivery.model.CompanyOrder;

public interface CompanyOrderRepository extends JpaRepository<CompanyOrder, Long> {
    List<CompanyOrder> findByCompany(Company company);

    List<CompanyOrder> findByCompanyOrderByOrderDateDesc(Company company);

    List<CompanyOrder> findByCompanyAndOrderDate(Company company, LocalDate orderDate);

    List<CompanyOrder> findByCompanyAndStatus(Company company, String status);

    List<CompanyOrder> findByMenuItem_Menu_Restaurant_IdOrderByOrderDateDesc(Long restaurantId);
}

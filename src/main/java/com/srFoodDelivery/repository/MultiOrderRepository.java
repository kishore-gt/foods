package com.srFoodDelivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.User;

@Repository
public interface MultiOrderRepository extends JpaRepository<MultiOrder, Long> {
    
    @Query("SELECT DISTINCT mo FROM MultiOrder mo LEFT JOIN FETCH mo.subOrders WHERE mo.user = :user ORDER BY mo.createdAt DESC")
    List<MultiOrder> findByUserOrderByCreatedAtDesc(@Param("user") User user);
    
    @Query("SELECT mo FROM MultiOrder mo LEFT JOIN FETCH mo.subOrders WHERE mo.id = :id AND mo.user = :user")
    Optional<MultiOrder> findByIdAndUser(@Param("id") Long id, @Param("user") User user);
    
    @Query("SELECT mo FROM MultiOrder mo WHERE mo.status = :status ORDER BY mo.createdAt DESC")
    List<MultiOrder> findByStatusOrderByCreatedAtDesc(@Param("status") String status);
    
    @Query("SELECT mo FROM MultiOrder mo WHERE mo.paymentStatus = :paymentStatus ORDER BY mo.createdAt DESC")
    List<MultiOrder> findByPaymentStatusOrderByCreatedAtDesc(@Param("paymentStatus") String paymentStatus);
}


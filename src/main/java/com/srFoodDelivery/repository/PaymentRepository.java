package com.srFoodDelivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByMultiOrder(MultiOrder multiOrder);
    
    List<Payment> findByPaymentStatusOrderByCreatedAtDesc(String paymentStatus);
    
    Optional<Payment> findByTransactionId(String transactionId);
}


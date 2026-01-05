package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.SubOrderItem;

@Repository
public interface SubOrderItemRepository extends JpaRepository<SubOrderItem, Long> {
    
    List<SubOrderItem> findBySubOrder(SubOrder subOrder);
}


package com.srFoodDelivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.srFoodDelivery.model.ChefProfile;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.Rider;
import com.srFoodDelivery.model.SubOrder;

@Repository
public interface SubOrderRepository extends JpaRepository<SubOrder, Long> {

        List<SubOrder> findByMultiOrderOrderByCreatedAtDesc(MultiOrder multiOrder);

        List<SubOrder> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant);

        List<SubOrder> findByChefProfileOrderByCreatedAtDesc(ChefProfile chefProfile);

        List<SubOrder> findByRiderOrderByCreatedAtDesc(Rider rider);

        @Query("SELECT so FROM SubOrder so WHERE so.restaurant = :restaurant AND so.status = :status ORDER BY so.createdAt DESC")
        List<SubOrder> findByRestaurantAndStatusOrderByCreatedAtDesc(
                        @Param("restaurant") Restaurant restaurant,
                        @Param("status") String status);

        @Query("SELECT so FROM SubOrder so WHERE so.rider = :rider AND so.status IN :statuses ORDER BY so.createdAt DESC")
        List<SubOrder> findByRiderAndStatusInOrderByCreatedAtDesc(
                        @Param("rider") Rider rider,
                        @Param("statuses") List<String> statuses);

        Optional<SubOrder> findByIdAndRestaurant(Long id, Restaurant restaurant);

        Optional<SubOrder> findByIdAndChefProfile(Long id, ChefProfile chefProfile);

        @Query("SELECT so FROM SubOrder so WHERE so.restaurant = :restaurant AND (so.preorderSlot IS NOT NULL OR so.multiOrder.orderingMode = 'PREORDER' OR so.multiOrder.orderingMode = 'DINE_IN') ORDER BY so.createdAt DESC")
        List<SubOrder> findPreorders(@Param("restaurant") Restaurant restaurant);

        @Query("SELECT so FROM SubOrder so WHERE so.restaurant = :restaurant AND so.preorderSlot IS NULL AND (so.multiOrder.orderingMode IS NULL OR (so.multiOrder.orderingMode != 'PREORDER' AND so.multiOrder.orderingMode != 'DINE_IN')) ORDER BY so.createdAt DESC")
        List<SubOrder> findDeliveryOrders(@Param("restaurant") Restaurant restaurant);

        @Query("SELECT so FROM SubOrder so WHERE (so.status = 'CONFIRMED' OR so.status = 'OFFERED') AND so.rider IS NULL AND so.preorderSlot IS NULL AND so.reservation IS NULL AND (so.multiOrder.orderingMode IS NULL OR so.multiOrder.orderingMode = 'DELIVERY') ORDER BY so.createdAt DESC")
        List<SubOrder> findAvailableOrdersForRiders();

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT so FROM SubOrder so WHERE so.id = :id")
        Optional<SubOrder> findByIdWithLock(@Param("id") Long id);
}

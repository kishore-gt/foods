package com.srFoodDelivery.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.PreorderSlot;
import com.srFoodDelivery.model.Restaurant;

@Repository
public interface PreorderSlotRepository extends JpaRepository<PreorderSlot, Long> {
    
    List<PreorderSlot> findByRestaurantAndIsActiveTrueOrderBySlotStartTimeAsc(Restaurant restaurant);
    
    @Query("SELECT ps FROM PreorderSlot ps WHERE ps.restaurant = :restaurant " +
           "AND ps.isActive = true " +
           "AND ps.slotStartTime >= :startTime " +
           "AND ps.maxCapacity > ps.currentCapacity " +
           "ORDER BY ps.slotStartTime ASC")
    List<PreorderSlot> findAvailableSlotsByRestaurant(
        @Param("restaurant") Restaurant restaurant,
        @Param("startTime") LocalDateTime startTime
    );
    
    @Query("SELECT ps FROM PreorderSlot ps WHERE ps.restaurant = :restaurant " +
           "AND ps.slotStartTime >= :startTime " +
           "AND ps.slotEndTime <= :endTime " +
           "AND ps.isActive = true " +
           "ORDER BY ps.slotStartTime ASC")
    List<PreorderSlot> findSlotsByRestaurantAndTimeRange(
        @Param("restaurant") Restaurant restaurant,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    Optional<PreorderSlot> findByIdAndIsActiveTrue(Long id);
    
    /**
     * Atomic reservation: Decrements max_capacity if capacity > 0
     * Returns number of rows affected (1 if successful, 0 if capacity exhausted)
     */
    @Modifying
    @Query(value = "UPDATE preorder_slot SET current_capacity = current_capacity + 1, " +
           "version = version + 1, updated_at = CURRENT_TIMESTAMP " +
           "WHERE id = :slotId AND current_capacity < max_capacity", nativeQuery = true)
    int reserveSlotAtomic(@Param("slotId") Long slotId);
    
    /**
     * Atomic release: Decrements current_capacity
     */
    @Modifying
    @Query(value = "UPDATE preorder_slot SET current_capacity = GREATEST(0, current_capacity - 1), " +
           "version = version + 1, updated_at = CURRENT_TIMESTAMP " +
           "WHERE id = :slotId", nativeQuery = true)
    int releaseSlotAtomic(@Param("slotId") Long slotId);
}


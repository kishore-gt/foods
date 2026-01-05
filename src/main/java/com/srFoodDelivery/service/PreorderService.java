package com.srFoodDelivery.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.PreorderSlot;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.repository.PreorderSlotRepository;
import com.srFoodDelivery.repository.RestaurantRepository;
import com.srFoodDelivery.repository.SubOrderRepository;

@Service
@Transactional
public class PreorderService {

    private static final Logger logger = LoggerFactory.getLogger(PreorderService.class);

    private final PreorderSlotRepository preorderSlotRepository;
    private final RestaurantRepository restaurantRepository;
    private final SubOrderRepository subOrderRepository;

    public PreorderService(
            PreorderSlotRepository preorderSlotRepository,
            RestaurantRepository restaurantRepository,
            SubOrderRepository subOrderRepository) {
        this.preorderSlotRepository = preorderSlotRepository;
        this.restaurantRepository = restaurantRepository;
        this.subOrderRepository = subOrderRepository;
    }

    /**
     * Atomically reserves a slot by decrementing capacity.
     * Returns true if reservation successful, false if capacity exhausted.
     */
    public boolean reserveSlot(Long slotId) {
        logger.info("Attempting to reserve slot: {}", slotId);
        
        int rowsAffected = preorderSlotRepository.reserveSlotAtomic(slotId);
        
        if (rowsAffected > 0) {
            logger.info("Successfully reserved slot: {}", slotId);
            return true;
        } else {
            logger.warn("Failed to reserve slot {} - capacity exhausted", slotId);
            return false;
        }
    }

    /**
     * Releases a slot reservation by incrementing capacity.
     */
    public boolean releaseSlot(Long slotId) {
        logger.info("Releasing slot: {}", slotId);
        
        int rowsAffected = preorderSlotRepository.releaseSlotAtomic(slotId);
        
        if (rowsAffected > 0) {
            logger.info("Successfully released slot: {}", slotId);
            return true;
        } else {
            logger.warn("Failed to release slot: {}", slotId);
            return false;
        }
    }

    /**
     * Reserves a slot for a specific suborder.
     */
    public boolean reserveSlotForSubOrder(Long slotId, Long subOrderId) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("SubOrder not found"));

        if (reserveSlot(slotId)) {
            PreorderSlot slot = preorderSlotRepository.findById(slotId)
                    .orElseThrow(() -> new IllegalArgumentException("PreorderSlot not found"));
            subOrder.setPreorderSlot(slot);
            subOrderRepository.save(subOrder);
            return true;
        }
        return false;
    }

    /**
     * Releases a slot reservation when suborder is cancelled.
     */
    public void releaseSlotForSubOrder(Long subOrderId) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("SubOrder not found"));

        if (subOrder.getPreorderSlot() != null) {
            Long slotId = subOrder.getPreorderSlot().getId();
            releaseSlot(slotId);
            subOrder.setPreorderSlot(null);
            subOrderRepository.save(subOrder);
        }
    }

    @Transactional(readOnly = true)
    public List<PreorderSlot> getAvailableSlotsByRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        LocalDateTime now = LocalDateTime.now();
        return preorderSlotRepository.findAvailableSlotsByRestaurant(restaurant, now);
    }

    @Transactional(readOnly = true)
    public List<PreorderSlot> getAllSlotsByRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        return preorderSlotRepository.findByRestaurantAndIsActiveTrueOrderBySlotStartTimeAsc(restaurant);
    }

    @Transactional(readOnly = true)
    public Optional<PreorderSlot> getSlotById(Long slotId) {
        return preorderSlotRepository.findById(slotId);
    }
}


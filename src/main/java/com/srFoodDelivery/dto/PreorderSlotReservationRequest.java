package com.srFoodDelivery.dto;

import jakarta.validation.constraints.NotNull;

public class PreorderSlotReservationRequest {

    @NotNull(message = "Restaurant ID is required")
    private Long restaurantId;

    @NotNull(message = "Slot ID is required")
    private Long slotId;

    @NotNull(message = "MultiOrder ID is required")
    private Long multiOrderId;

    @NotNull(message = "SubOrder ID is required")
    private Long subOrderId;

    // Getters and Setters
    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public Long getMultiOrderId() {
        return multiOrderId;
    }

    public void setMultiOrderId(Long multiOrderId) {
        this.multiOrderId = multiOrderId;
    }

    public Long getSubOrderId() {
        return subOrderId;
    }

    public void setSubOrderId(Long subOrderId) {
        this.subOrderId = subOrderId;
    }
}


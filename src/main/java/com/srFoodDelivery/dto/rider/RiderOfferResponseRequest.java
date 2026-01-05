package com.srFoodDelivery.dto.rider;

import jakarta.validation.constraints.NotNull;

public class RiderOfferResponseRequest {

    @NotNull(message = "SubOrder ID is required")
    private Long subOrderId;

    @NotNull(message = "Accept flag is required")
    private Boolean accept;

    // Getters and Setters
    public Long getSubOrderId() {
        return subOrderId;
    }

    public void setSubOrderId(Long subOrderId) {
        this.subOrderId = subOrderId;
    }

    public Boolean getAccept() {
        return accept;
    }

    public void setAccept(Boolean accept) {
        this.accept = accept;
    }
}


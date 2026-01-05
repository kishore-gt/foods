package com.srFoodDelivery.dto.rider;

import jakarta.validation.constraints.NotBlank;

public class RiderStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}


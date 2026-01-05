package com.srFoodDelivery.dto.payment;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class DemoPaymentRequest {

    @NotNull(message = "MultiOrder ID is required")
    private Long multiOrderId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    // Getters and Setters
    public Long getMultiOrderId() {
        return multiOrderId;
    }

    public void setMultiOrderId(Long multiOrderId) {
        this.multiOrderId = multiOrderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}


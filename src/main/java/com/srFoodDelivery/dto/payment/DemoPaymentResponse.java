package com.srFoodDelivery.dto.payment;

import java.math.BigDecimal;

public class DemoPaymentResponse {

    private Long paymentId;
    private String paymentStatus;
    private Boolean requiresOtp;
    private String message;
    private BigDecimal amount;

    // Getters and Setters
    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Boolean getRequiresOtp() {
        return requiresOtp;
    }

    public void setRequiresOtp(Boolean requiresOtp) {
        this.requiresOtp = requiresOtp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}


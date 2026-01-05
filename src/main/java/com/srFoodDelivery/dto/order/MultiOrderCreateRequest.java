package com.srFoodDelivery.dto.order;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MultiOrderCreateRequest {

    @NotEmpty(message = "Cart items are required")
    @Valid
    private List<CartItemRequest> cartItems;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 200, message = "Delivery address must not exceed 200 characters")
    private String deliveryAddress;

    @Size(max = 500, message = "Special instructions must not exceed 500 characters")
    private String specialInstructions;

    private BigDecimal discountAmount;

    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    private String appliedCoupon;

    private Long preorderSlotId;

    private Long reservationId;

    // Getters and Setters
    public List<CartItemRequest> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItemRequest> cartItems) {
        this.cartItems = cartItems;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getAppliedCoupon() {
        return appliedCoupon;
    }

    public void setAppliedCoupon(String appliedCoupon) {
        this.appliedCoupon = appliedCoupon;
    }

    public Long getPreorderSlotId() {
        return preorderSlotId;
    }

    public void setPreorderSlotId(Long preorderSlotId) {
        this.preorderSlotId = preorderSlotId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public static class CartItemRequest {
        @NotNull(message = "Menu item ID is required")
        private Long menuItemId;

        @NotNull(message = "Quantity is required")
        private Integer quantity;

        // Getters and Setters
        public Long getMenuItemId() {
            return menuItemId;
        }

        public void setMenuItemId(Long menuItemId) {
            this.menuItemId = menuItemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}


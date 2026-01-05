package com.srFoodDelivery.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MultiOrderDTO {

    private Long id;
    private Long userId;
    private String userName;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private String appliedCoupon;
    private String status;
    private String deliveryAddress;
    private String specialInstructions;
    private String paymentStatus;
    private List<SubOrderDTO> subOrders = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public List<SubOrderDTO> getSubOrders() {
        return subOrders;
    }

    public void setSubOrders(List<SubOrderDTO> subOrders) {
        this.subOrders = subOrders;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class SubOrderDTO {
        private Long id;
        private Long restaurantId;
        private String restaurantName;
        private String restaurantLat;
        private String restaurantLon;
        private Long chefProfileId;
        private String chefName;
        private String status;
        private BigDecimal totalAmount;
        private Long riderId;
        private String riderName;
        private Long preorderSlotId;
        private LocalDateTime preorderSlotStartTime;
        private LocalDateTime preorderSlotEndTime;
        private Long reservationId;
        private Long tableId;
        private String tableName;
        private String tableNumber;
        private String reservationDate;
        private String reservationTime;
        private Integer durationMinutes;
        private Integer numberOfGuests;
        private String orderType; // DELIVERY, DINE_IN, TAKEAWAY
        private LocalDateTime estimatedDeliveryTime;
        private LocalDateTime actualDeliveryTime;
        private String trackingInfo;
        private List<SubOrderItemDTO> items = new ArrayList<>();

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getRestaurantId() {
            return restaurantId;
        }

        public void setRestaurantId(Long restaurantId) {
            this.restaurantId = restaurantId;
        }

        public String getRestaurantName() {
            return restaurantName;
        }

        public void setRestaurantName(String restaurantName) {
            this.restaurantName = restaurantName;
        }

        public String getRestaurantLat() {
            return restaurantLat;
        }

        public void setRestaurantLat(String restaurantLat) {
            this.restaurantLat = restaurantLat;
        }

        public String getRestaurantLon() {
            return restaurantLon;
        }

        public void setRestaurantLon(String restaurantLon) {
            this.restaurantLon = restaurantLon;
        }

        public Long getChefProfileId() {
            return chefProfileId;
        }

        public void setChefProfileId(Long chefProfileId) {
            this.chefProfileId = chefProfileId;
        }

        public String getChefName() {
            return chefName;
        }

        public void setChefName(String chefName) {
            this.chefName = chefName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public Long getRiderId() {
            return riderId;
        }

        public void setRiderId(Long riderId) {
            this.riderId = riderId;
        }

        public String getRiderName() {
            return riderName;
        }

        public void setRiderName(String riderName) {
            this.riderName = riderName;
        }

        public Long getPreorderSlotId() {
            return preorderSlotId;
        }

        public void setPreorderSlotId(Long preorderSlotId) {
            this.preorderSlotId = preorderSlotId;
        }

        public LocalDateTime getPreorderSlotStartTime() {
            return preorderSlotStartTime;
        }

        public void setPreorderSlotStartTime(LocalDateTime preorderSlotStartTime) {
            this.preorderSlotStartTime = preorderSlotStartTime;
        }

        public LocalDateTime getPreorderSlotEndTime() {
            return preorderSlotEndTime;
        }

        public void setPreorderSlotEndTime(LocalDateTime preorderSlotEndTime) {
            this.preorderSlotEndTime = preorderSlotEndTime;
        }

        public Long getReservationId() {
            return reservationId;
        }

        public void setReservationId(Long reservationId) {
            this.reservationId = reservationId;
        }

        public Long getTableId() {
            return tableId;
        }

        public void setTableId(Long tableId) {
            this.tableId = tableId;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getTableNumber() {
            return tableNumber;
        }

        public void setTableNumber(String tableNumber) {
            this.tableNumber = tableNumber;
        }

        public String getReservationDate() {
            return reservationDate;
        }

        public void setReservationDate(String reservationDate) {
            this.reservationDate = reservationDate;
        }

        public String getReservationTime() {
            return reservationTime;
        }

        public void setReservationTime(String reservationTime) {
            this.reservationTime = reservationTime;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public Integer getNumberOfGuests() {
            return numberOfGuests;
        }

        public void setNumberOfGuests(Integer numberOfGuests) {
            this.numberOfGuests = numberOfGuests;
        }

        public String getOrderType() {
            return orderType;
        }

        public void setOrderType(String orderType) {
            this.orderType = orderType;
        }

        public LocalDateTime getEstimatedDeliveryTime() {
            return estimatedDeliveryTime;
        }

        public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
            this.estimatedDeliveryTime = estimatedDeliveryTime;
        }

        public LocalDateTime getActualDeliveryTime() {
            return actualDeliveryTime;
        }

        public void setActualDeliveryTime(LocalDateTime actualDeliveryTime) {
            this.actualDeliveryTime = actualDeliveryTime;
        }

        public String getTrackingInfo() {
            return trackingInfo;
        }

        public void setTrackingInfo(String trackingInfo) {
            this.trackingInfo = trackingInfo;
        }

        public List<SubOrderItemDTO> getItems() {
            return items;
        }

        public void setItems(List<SubOrderItemDTO> items) {
            this.items = items;
        }
    }

    public static class SubOrderItemDTO {
        private Long id;
        private Long menuItemId;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getMenuItemId() {
            return menuItemId;
        }

        public void setMenuItemId(Long menuItemId) {
            this.menuItemId = menuItemId;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getLineTotal() {
            return lineTotal;
        }

        public void setLineTotal(BigDecimal lineTotal) {
            this.lineTotal = lineTotal;
        }
    }
}


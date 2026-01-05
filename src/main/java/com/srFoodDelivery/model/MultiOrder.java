package com.srFoodDelivery.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "multi_order")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MultiOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "multiOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubOrder> subOrders = new ArrayList<>();

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Size(max = 50)
    @Column(name = "applied_coupon", length = 50)
    private String appliedCoupon;

    @Column(length = 30, nullable = false)
    private String status = "PENDING";

    @NotBlank
    @Size(max = 200)
    @Column(name = "delivery_address", nullable = false, length = 200)
    private String deliveryAddress;

    @Size(max = 500)
    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    @Column(name = "payment_status", length = 30)
    private String paymentStatus = "PENDING";

    @Size(max = 50)
    @Column(name = "payment_method", length = 50)
    private String paymentMethod = "ONLINE"; // ONLINE, AT_RESTAURANT, SPLIT

    @Column(name = "payment_at_restaurant")
    private Boolean paymentAtRestaurant = false;

    @Size(max = 20)
    @Column(name = "ordering_mode", length = 20)
    private String orderingMode = "DELIVERY"; // DELIVERY, TAKEAWAY, DINE_IN, PREORDER

    @Column(name = "scheduled_delivery_time")
    private LocalDateTime scheduledDeliveryTime; // For preorders and scheduled deliveries

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<SubOrder> getSubOrders() {
        return subOrders;
    }

    public void setSubOrders(List<SubOrder> subOrders) {
        this.subOrders = subOrders;
    }

    public void addSubOrder(SubOrder subOrder) {
        subOrder.setMultiOrder(this);
        this.subOrders.add(subOrder);
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

    public String getOrderingMode() {
        return orderingMode;
    }

    public void setOrderingMode(String orderingMode) {
        this.orderingMode = orderingMode;
    }

    public LocalDateTime getScheduledDeliveryTime() {
        return scheduledDeliveryTime;
    }

    public void setScheduledDeliveryTime(LocalDateTime scheduledDeliveryTime) {
        this.scheduledDeliveryTime = scheduledDeliveryTime;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Boolean getPaymentAtRestaurant() {
        return paymentAtRestaurant;
    }

    public void setPaymentAtRestaurant(Boolean paymentAtRestaurant) {
        this.paymentAtRestaurant = paymentAtRestaurant;
    }
}


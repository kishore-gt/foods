package com.srFoodDelivery.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "offer")
public class Offer {

    public enum OfferType {
        PERCENTAGE_OFF,
        BUY_ONE_GET_ONE,
        FLAT_DISCOUNT,
        FREE_DELIVERY,
        COMBO_OFFER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Size(max = 50)
    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @NotBlank
    @Column(name = "offer_type", nullable = false, length = 50)
    private String offerType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "applicable_menu_item_ids", length = 1000)
    private String applicableMenuItemIds;

    @NotNull
    @Column(name = "start_date", nullable = false)
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Size(max = 500)
    @Column(name = "image_url", length = 500)
    private String imageUrl;

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

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getOfferType() {
        return offerType;
    }

    public void setOfferType(String offerType) {
        this.offerType = offerType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public String getApplicableMenuItemIds() {
        return applicableMenuItemIds;
    }

    public void setApplicableMenuItemIds(String applicableMenuItemIds) {
        this.applicableMenuItemIds = applicableMenuItemIds;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && now.isAfter(startDate) && now.isBefore(endDate);
    }

    public List<Long> getApplicableMenuItemIdList() {
        List<Long> ids = new ArrayList<>();
        if (applicableMenuItemIds != null && !applicableMenuItemIds.trim().isEmpty()) {
            String[] parts = applicableMenuItemIds.split(",");
            for (String part : parts) {
                try {
                    ids.add(Long.parseLong(part.trim()));
                } catch (NumberFormatException e) {
                    // Skip invalid IDs
                }
            }
        }
        return ids;
    }

    public void setApplicableMenuItemIdList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            this.applicableMenuItemIds = null;
        } else {
            this.applicableMenuItemIds = ids.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
        }
    }
}

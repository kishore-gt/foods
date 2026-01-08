package com.srFoodDelivery.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "menu_items")
public class MenuItem {

    public MenuItem() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean available = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "menu_item_tags", joinColumns = @JoinColumn(name = "menu_item_id"))
    @Column(name = "tag", length = 30)
    private Set<String> tags = new HashSet<>();

    @Size(max = 500)
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Size(max = 50)
    @Column(length = 50)
    private String category; // DESSERTS, STARTERS, BEVERAGES, SNACKS, etc.

    @Column(name = "is_veg", nullable = false)
    private boolean isVeg = true; // true for veg, false for non-veg

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isVeg() {
        return isVeg;
    }

    public void setVeg(boolean isVeg) {
        this.isVeg = isVeg;
    }

    public BigDecimal getEffectivePrice() {
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return price;
        }
        BigDecimal discountFactor = BigDecimal.valueOf(100).subtract(discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        return price.multiply(discountFactor).setScale(2, java.math.RoundingMode.HALF_UP);

    }

    // For display in templates
    public boolean hasDiscount() {
        return discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0;
    }

    @jakarta.persistence.Transient
    private String offerDisplayLabel;

    public String getOfferDisplayLabel() {
        return offerDisplayLabel;
    }

    public void setOfferDisplayLabel(String offerDisplayLabel) {
        this.offerDisplayLabel = offerDisplayLabel;
    }
}

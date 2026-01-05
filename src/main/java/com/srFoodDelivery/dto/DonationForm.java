package com.srFoodDelivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DonationForm {
    
    @NotBlank
    @Size(max = 200)
    private String foodName;
    
    @NotNull
    @Min(1)
    private Integer quantity;
    
    @Size(max = 500)
    private String description;
    
    @NotNull
    private Boolean inGoodCondition = true;

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getInGoodCondition() {
        return inGoodCondition;
    }

    public void setInGoodCondition(Boolean inGoodCondition) {
        this.inGoodCondition = inGoodCondition;
    }
}


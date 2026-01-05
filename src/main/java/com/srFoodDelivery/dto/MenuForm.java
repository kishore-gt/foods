package com.srFoodDelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MenuForm {

    @NotBlank
    @Size(max = 120)
    private String title;

    @Size(max = 500)
    private String description;

    @NotBlank
    private String type; // RESTAURANT or CHEF

    private Long restaurantId;
    private Long chefProfileId;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Long getChefProfileId() {
        return chefProfileId;
    }

    public void setChefProfileId(Long chefProfileId) {
        this.chefProfileId = chefProfileId;
    }
}

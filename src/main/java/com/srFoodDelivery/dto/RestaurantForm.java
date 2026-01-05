package com.srFoodDelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RestaurantForm {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 200)
    private String address;

    @Size(max = 100)
    private String contactNumber;

    @Size(max = 500)
    private String imageUrl;

    @Size(max = 10)
    private String openingTime;

    @Size(max = 10)
    private String closingTime;

    @NotBlank
    private String businessType = "RESTAURANT"; // RESTAURANT or CAFE

    // Table configuration fields
    private Integer numberOfTables;
    private Integer defaultTableCapacity; // Default seating per table (2, 4, 6, etc.)
    private String tableLayout; // SIMPLE, GRID, CUSTOM
    private Integer tablesPerRow; // For grid layout
    private Boolean hasDineIn; // Whether restaurant has dine-in facility

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(String openingTime) {
        this.openingTime = openingTime;
    }

    public String getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(String closingTime) {
        this.closingTime = closingTime;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public Integer getNumberOfTables() {
        return numberOfTables;
    }

    public void setNumberOfTables(Integer numberOfTables) {
        this.numberOfTables = numberOfTables;
    }

    public Integer getDefaultTableCapacity() {
        return defaultTableCapacity;
    }

    public void setDefaultTableCapacity(Integer defaultTableCapacity) {
        this.defaultTableCapacity = defaultTableCapacity;
    }

    public String getTableLayout() {
        return tableLayout;
    }

    public void setTableLayout(String tableLayout) {
        this.tableLayout = tableLayout;
    }

    public Integer getTablesPerRow() {
        return tablesPerRow;
    }

    public void setTablesPerRow(Integer tablesPerRow) {
        this.tablesPerRow = tablesPerRow;
    }

    public Boolean getHasDineIn() {
        return hasDineIn;
    }

    public void setHasDineIn(Boolean hasDineIn) {
        this.hasDineIn = hasDineIn;
    }

}

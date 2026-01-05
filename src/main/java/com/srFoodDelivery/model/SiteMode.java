package com.srFoodDelivery.model;

/**
 * Represents the visual/functional mode that the storefront is operating in.
 * Mode selection is shared across the entire session so that the customer can
 * quickly switch between the primary restaurant flow and the cafe-only
 * experience without losing their cart or filters.
 */
public enum SiteMode {
    RESTAURANT(
            "Restaurant Mode",
            "Discover full-service restaurants for delivery, dine-in, and preorders."),
    CAFE(
            "Cafe Mode",
            "Browse quick-serve cafes, bakeries, and beverage-only spots with preorder support.");

    public static final String SESSION_ATTRIBUTE = "siteMode";

    private final String displayName;
    private final String description;

    SiteMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCafeMode() {
        return this == CAFE;
    }

    /**
     * Resolve incoming values from query params or session attributes into a
     * SiteMode. Supports numeric shortcuts ("1" => RESTAURANT, "2" => CAFE) as
     * well as explicit names.
     */
    public static SiteMode fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        switch (normalized) {
            case "CAFE":
            case "2":
                return CAFE;
            case "RESTAURANT":
            case "RESTAURANT_MODE":
            case "1":
                return RESTAURANT;
            default:
                return null;
        }
    }
}


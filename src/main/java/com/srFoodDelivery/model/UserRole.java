package com.srFoodDelivery.model;

/**
 * Central place to keep role identifiers that Spring Security will use.
 */
public final class UserRole {

    private UserRole() {
        // utility class
    }

    public static final String CUSTOMER = "CUSTOMER";
    public static final String OWNER    = "OWNER";
    public static final String CAFE_OWNER = "CAFE_OWNER";
    public static final String RIDER    = "RIDER";
    public static final String ADMIN    = "ADMIN";
    public static final String COMPANY  = "COMPANY";
    
    @Deprecated
    public static final String CHEF     = "CHEF"; // Deprecated - use RIDER instead
}
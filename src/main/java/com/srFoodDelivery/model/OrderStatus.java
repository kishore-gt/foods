package com.srFoodDelivery.model;

public final class OrderStatus {

    private OrderStatus() {
    }

    public static final String NEW = "NEW";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String PREPARING = "PREPARING";
    public static final String OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String DELIVERED = "DELIVERED";
    public static final String CANCELLED = "CANCELLED";
}

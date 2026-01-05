package com.srFoodDelivery.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RestaurantTag {

    private RestaurantTag() {
    }

    public static final String BREAKFAST = "BREAKFAST";
    public static final String LUNCH = "LUNCH";
    public static final String SNACKS = "SNACKS";
    public static final String DINNER = "DINNER";
    public static final String RAINY = "RAINY";
    public static final String SUMMER = "SUMMER";
    public static final String WINTER = "WINTER";
    public static final String VEG = "VEG";
    public static final String NONVEG = "NONVEG";
    public static final String JUICES = "JUICES";
    public static final String ICECREAMS = "ICECREAMS";

    private static final Map<String, String> LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(BREAKFAST, "Breakfast");
        labels.put(LUNCH, "Lunch");
        labels.put(SNACKS, "Snacks");
        labels.put(DINNER, "Dinner");
        labels.put(RAINY, "Rainy");
        labels.put(SUMMER, "Summer");
        labels.put(WINTER, "Winter");
        labels.put(VEG, "Veg");
        labels.put(NONVEG, "Non Veg");
        labels.put(JUICES, "Juices");
        labels.put(ICECREAMS, "Ice Creams");
        LABELS = Collections.unmodifiableMap(labels);
    }

    public static Map<String, String> allTags() {
        return LABELS;
    }

    public static Set<String> keys() {
        return LABELS.keySet();
    }

    public static String labelFor(String key) {
        return LABELS.getOrDefault(key, key);
    }
}

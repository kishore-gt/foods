package com.srFoodDelivery.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ImageService {

    private static final Map<String, String> FOOD_IMAGE_MAP = new HashMap<>();
    
    static {
        // Indian food images from Unsplash
        FOOD_IMAGE_MAP.put("idli", "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400");
        FOOD_IMAGE_MAP.put("dosa", "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=400");
        FOOD_IMAGE_MAP.put("vada", "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=400");
        FOOD_IMAGE_MAP.put("sambar", "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400");
        FOOD_IMAGE_MAP.put("biryani", "https://images.unsplash.com/photo-1631200098866-4c8f59b0e3c1?w=400");
        FOOD_IMAGE_MAP.put("pulao", "https://images.unsplash.com/photo-1631200098866-4c8f59b0e3c1?w=400");
        FOOD_IMAGE_MAP.put("curry", "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400");
        FOOD_IMAGE_MAP.put("dal", "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400");
        FOOD_IMAGE_MAP.put("roti", "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=400");
        FOOD_IMAGE_MAP.put("naan", "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=400");
        FOOD_IMAGE_MAP.put("paratha", "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=400");
        FOOD_IMAGE_MAP.put("paneer", "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400");
        FOOD_IMAGE_MAP.put("tikka", "https://images.unsplash.com/photo-1631200098866-4c8f59b0e3c1?w=400");
        FOOD_IMAGE_MAP.put("kebab", "https://images.unsplash.com/photo-1631200098866-4c8f59b0e3c1?w=400");
        FOOD_IMAGE_MAP.put("samosa", "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=400");
        FOOD_IMAGE_MAP.put("pakora", "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=400");
        FOOD_IMAGE_MAP.put("chai", "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=400");
        FOOD_IMAGE_MAP.put("coffee", "https://images.unsplash.com/photo-1511920170033-83939cdc2da7?w=400");
        FOOD_IMAGE_MAP.put("juice", "https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=400");
        FOOD_IMAGE_MAP.put("smoothie", "https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=400");
        FOOD_IMAGE_MAP.put("ice cream", "https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=400");
        FOOD_IMAGE_MAP.put("burger", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400");
        FOOD_IMAGE_MAP.put("pizza", "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400");
        FOOD_IMAGE_MAP.put("pasta", "https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=400");
        FOOD_IMAGE_MAP.put("salad", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400");
        FOOD_IMAGE_MAP.put("sandwich", "https://images.unsplash.com/photo-1509722747041-616f39b57569?w=400");
        FOOD_IMAGE_MAP.put("soup", "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=400");
        FOOD_IMAGE_MAP.put("rice", "https://images.unsplash.com/photo-1631200098866-4c8f59b0e3c1?w=400");
        FOOD_IMAGE_MAP.put("noodles", "https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=400");
        FOOD_IMAGE_MAP.put("fried rice", "https://images.unsplash.com/photo-1631200098866-4c8f59b0e3c1?w=400");
        FOOD_IMAGE_MAP.put("chicken", "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=400");
        FOOD_IMAGE_MAP.put("mutton", "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=400");
        FOOD_IMAGE_MAP.put("fish", "https://images.unsplash.com/photo-1544943910-4c1dc44aab44?w=400");
        FOOD_IMAGE_MAP.put("prawn", "https://images.unsplash.com/photo-1544943910-4c1dc44aab44?w=400");
    }

    public String getImageUrlForFood(String foodName) {
        if (foodName == null || foodName.trim().isEmpty()) {
            return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400";
        }
        
        String lowerName = foodName.toLowerCase().trim();
        
        // Check for exact match
        if (FOOD_IMAGE_MAP.containsKey(lowerName)) {
            return FOOD_IMAGE_MAP.get(lowerName);
        }
        
        // Check for partial matches
        for (Map.Entry<String, String> entry : FOOD_IMAGE_MAP.entrySet()) {
            if (lowerName.contains(entry.getKey()) || entry.getKey().contains(lowerName)) {
                return entry.getValue();
            }
        }
        
        // Default food image
        return "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400";
    }

    public String getImageUrlForRestaurant(String restaurantName) {
        if (restaurantName == null || restaurantName.trim().isEmpty()) {
            return "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600";
        }
        return "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=600";
    }
}


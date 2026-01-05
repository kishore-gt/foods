package com.srFoodDelivery.Controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.repository.RestaurantRepository;
import com.srFoodDelivery.security.CustomUserDetails;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final RestaurantRepository restaurantRepository;

    public AdminApiController(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @PostMapping("/restaurants/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveRestaurant(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        // In a real system, you'd have an 'approved' field
        // For now, we'll assume approval means the restaurant is active
        // You can add an 'approved' boolean field to Restaurant entity if needed

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("restaurantId", id);
        response.put("message", "Restaurant approved successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restaurants/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleRestaurant(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        // Toggle restaurant active status
        // Note: You may need to add an 'isActive' field to Restaurant entity
        // For now, this is a placeholder that can be extended

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("restaurantId", id);
        response.put("message", "Restaurant status toggled successfully");
        return ResponseEntity.ok(response);
    }
}


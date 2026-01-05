package com.srFoodDelivery.Controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srFoodDelivery.dto.PreorderSlotReservationRequest;
import com.srFoodDelivery.model.PreorderSlot;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.PreorderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/preorder")
public class PreorderApiController {

    private final PreorderService preorderService;

    public PreorderApiController(PreorderService preorderService) {
        this.preorderService = preorderService;
    }

    @GetMapping("/restaurants/{restaurantId}/slots")
    public ResponseEntity<List<PreorderSlot>> getPreorderSlots(@PathVariable Long restaurantId) {
        List<PreorderSlot> slots = preorderService.getAvailableSlotsByRestaurant(restaurantId);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveSlot(
            @Valid @RequestBody PreorderSlotReservationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        boolean reserved = preorderService.reserveSlotForSubOrder(
                request.getSlotId(), request.getSubOrderId());
        
        if (reserved) {
            response.put("success", true);
            response.put("message", "Slot reserved successfully");
            response.put("slotId", request.getSlotId());
            response.put("subOrderId", request.getSubOrderId());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Slot capacity exhausted or invalid slot");
            return ResponseEntity.status(409).body(response); // Conflict
        }
    }
}


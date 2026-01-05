package com.srFoodDelivery.Controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srFoodDelivery.dto.rider.RiderLocationUpdateRequest;
import com.srFoodDelivery.dto.rider.RiderOfferResponseRequest;
import com.srFoodDelivery.dto.rider.RiderStatusUpdateRequest;
import com.srFoodDelivery.model.Rider;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.rider.RiderService;
import com.srFoodDelivery.repository.RiderRepository;
import com.srFoodDelivery.repository.SubOrderRepository;
import com.srFoodDelivery.websocket.OrderWebSocketPublisher;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/riders")
public class RiderApiController {

    private final RiderService riderService;
    private final RiderRepository riderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderWebSocketPublisher webSocketPublisher;

    public RiderApiController(
            RiderService riderService,
            RiderRepository riderRepository,
            SubOrderRepository subOrderRepository,
            OrderWebSocketPublisher webSocketPublisher) {
        this.riderService = riderService;
        this.riderRepository = riderRepository;
        this.subOrderRepository = subOrderRepository;
        this.webSocketPublisher = webSocketPublisher;
    }

    @PostMapping("/{id}/toggle-online")
    public ResponseEntity<Map<String, Object>> toggleOnline(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        Rider rider = riderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rider not found"));

        // Verify rider belongs to authenticated user
        if (!rider.getUser().getId().equals(principal.getUser().getId())) {
            throw new SecurityException("Access denied");
        }

        Boolean isOnline = request.get("isOnline");
        if (isOnline == null) {
            isOnline = !rider.getIsOnline(); // Toggle if not specified
        }

        riderService.toggleOnlineStatus(id, isOnline);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("riderId", id);
        response.put("isOnline", isOnline);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/location")
    public ResponseEntity<Map<String, Object>> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody RiderLocationUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        Rider rider = riderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rider not found"));

        // Verify rider belongs to authenticated user
        if (!rider.getUser().getId().equals(principal.getUser().getId())) {
            throw new SecurityException("Access denied");
        }

        riderService.updateRiderLocation(id, request.getLat(), request.getLon());

        // Broadcast location update for active orders
        if (rider.getAssignedOrders() != null && !rider.getAssignedOrders().isEmpty()) {
            for (SubOrder subOrder : rider.getAssignedOrders()) {
                if (subOrder.getMultiOrder() != null) {
                    webSocketPublisher.publishRiderLocation(
                            subOrder.getMultiOrder().getId(),
                            id,
                            request.getLat().doubleValue(),
                            request.getLon().doubleValue());
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("riderId", id);
        response.put("lat", request.getLat());
        response.put("lon", request.getLon());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/offer-response")
    public ResponseEntity<Map<String, Object>> offerResponse(
            @PathVariable Long id,
            @Valid @RequestBody RiderOfferResponseRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        Rider rider = riderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rider not found"));

        // Verify rider belongs to authenticated user
        if (!rider.getUser().getId().equals(principal.getUser().getId())) {
            throw new SecurityException("Access denied");
        }

        boolean accepted = riderService.handleRiderOfferResponse(
                id, request.getSubOrderId(), request.getAccept());

        SubOrder subOrder = subOrderRepository.findById(request.getSubOrderId())
                .orElseThrow(() -> new IllegalArgumentException("SubOrder not found"));

        // Broadcast update
        if (accepted) {
            webSocketPublisher.publishOrderUpdate(
                    subOrder.getMultiOrder().getId(),
                    subOrder.getId(),
                    subOrder.getStatus(),
                    id,
                    rider.getCurrentLatitude() != null ? rider.getCurrentLatitude().doubleValue() : null,
                    rider.getCurrentLongitude() != null ? rider.getCurrentLongitude().doubleValue() : null);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", accepted);
        response.put("riderId", id);
        response.put("subOrderId", request.getSubOrderId());
        response.put("accepted", request.getAccept());
        response.put("message", accepted ? "Offer accepted" : "Offer declined");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RiderStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        
        Rider rider = riderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rider not found"));

        // Verify rider belongs to authenticated user
        if (!rider.getUser().getId().equals(principal.getUser().getId())) {
            throw new SecurityException("Access denied");
        }

        riderService.updateRiderStatus(id, request.getStatus());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("riderId", id);
        response.put("status", request.getStatus());
        return ResponseEntity.ok(response);
    }
}


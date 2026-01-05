package com.srFoodDelivery.Controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srFoodDelivery.dto.order.MultiOrderCreateRequest;
import com.srFoodDelivery.dto.order.MultiOrderDTO;
import com.srFoodDelivery.model.Order;
import com.srFoodDelivery.security.CustomUserDetails;
import com.srFoodDelivery.service.OrderService;
import com.srFoodDelivery.service.order.OrderOrchestrationService;
import com.srFoodDelivery.repository.MenuItemRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final OrderOrchestrationService orderOrchestrationService;

    public OrderApiController(
            OrderService orderService,
            OrderOrchestrationService orderOrchestrationService) {
        this.orderService = orderService;
        this.orderOrchestrationService = orderOrchestrationService;
    }

    // Legacy endpoints (backward compatibility)
    @GetMapping("/my-orders")
    public List<Order> getMyOrders(@AuthenticationPrincipal CustomUserDetails principal) {
        return orderService.getOrderHistory(principal.getUser());
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        Order order = orderService.getOrder(id);
        if (!order.getUser().getId().equals(principal.getUser().getId())) {
            throw new SecurityException("Access denied");
        }
        return order;
    }

    @GetMapping("/{id}/track")
    public Order trackOrder(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        Order order = orderService.getOrder(id);
        if (!order.getUser().getId().equals(principal.getUser().getId())) {
            throw new SecurityException("Access denied");
        }
        return order;
    }

    // New MultiOrder endpoints
    @PostMapping
    public ResponseEntity<MultiOrderDTO> createMultiOrder(
            @Valid @RequestBody MultiOrderCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        MultiOrderDTO multiOrder = orderOrchestrationService.createMultiOrder(
                principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(multiOrder);
    }

    @GetMapping("/multi/{id}")
    public ResponseEntity<MultiOrderDTO> getMultiOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        MultiOrderDTO multiOrder = orderOrchestrationService.getMultiOrderById(
                id, principal.getUser());
        return ResponseEntity.ok(multiOrder);
    }

    @GetMapping("/multi")
    public ResponseEntity<List<MultiOrderDTO>> getMyMultiOrders(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<MultiOrderDTO> multiOrders = orderOrchestrationService.getUserMultiOrders(
                principal.getUser());
        return ResponseEntity.ok(multiOrders);
    }
}

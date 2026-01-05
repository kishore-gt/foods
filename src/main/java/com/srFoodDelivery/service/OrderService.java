package com.srFoodDelivery.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.dto.order.MultiOrderCreateRequest;
import com.srFoodDelivery.model.Cart;
import com.srFoodDelivery.model.CartItem;
import com.srFoodDelivery.model.Order;
import com.srFoodDelivery.model.OrderItem;
import com.srFoodDelivery.model.OrderStatus;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.OrderRepository;
import com.srFoodDelivery.service.order.OrderOrchestrationService;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final EmailService emailService;
    private final OrderOrchestrationService orderOrchestrationService;

    public OrderService(OrderRepository orderRepository, CartService cartService, 
                       EmailService emailService, OrderOrchestrationService orderOrchestrationService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.emailService = emailService;
        this.orderOrchestrationService = orderOrchestrationService;
    }

    public Order placeOrder(User user, String deliveryAddress, String specialInstructions) {
        return placeOrder(user, deliveryAddress, specialInstructions, null, BigDecimal.ZERO, null, null);
    }

    public Order placeOrder(User user, String deliveryAddress, String specialInstructions, 
                           String deliveryLocation, BigDecimal discountAmount, String appliedCoupon) {
        return placeOrder(user, deliveryAddress, specialInstructions, deliveryLocation, discountAmount, appliedCoupon, null);
    }

    public Order placeOrder(User user, String deliveryAddress, String specialInstructions, 
                           String deliveryLocation, BigDecimal discountAmount, String appliedCoupon, Long preorderSlotId) {
        return placeOrder(user, deliveryAddress, specialInstructions, deliveryLocation, discountAmount, appliedCoupon, preorderSlotId, null);
    }

    public Order placeOrder(User user, String deliveryAddress, String specialInstructions, 
                           String deliveryLocation, BigDecimal discountAmount, String appliedCoupon, Long preorderSlotId, Long reservationId) {
        Cart cart = cartService.findCart(user)
                .orElseThrow(() -> new IllegalStateException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        
        // Use MultiOrder system for ALL orders (both single and multi-restaurant)
        // Convert cart to MultiOrderCreateRequest and create MultiOrder
        MultiOrderCreateRequest request = convertCartToMultiOrderRequest(cart, deliveryAddress, 
                specialInstructions, discountAmount, appliedCoupon, preorderSlotId, reservationId);
        orderOrchestrationService.createMultiOrder(user, request);
        cartService.clearCart(cart);
        
        // For backward compatibility, return a dummy Order object
        // In practice, the MultiOrder is created and can be retrieved via API
        Order dummyOrder = new Order();
        dummyOrder.setUser(user);
        dummyOrder.setDeliveryAddress(deliveryAddress);
        dummyOrder.setSpecialInstructions(specialInstructions);
        dummyOrder.setStatus(OrderStatus.NEW);
        dummyOrder.setTotalAmount(cart.getTotalAmount().subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO));
        dummyOrder.setDiscountAmount(discountAmount);
        dummyOrder.setAppliedCoupon(appliedCoupon);
        
        // Send email notification (using dummy order for email template compatibility)
        emailService.sendOrderConfirmationEmail(user, dummyOrder);
        
        return dummyOrder;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrderHistory(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Order updateStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);
        
        // Get the customer user from the order (ensure it's loaded)
        User customer = savedOrder.getUser();
        if (customer != null) {
            // Send status update email to the customer
            emailService.sendOrderStatusUpdateEmail(customer, savedOrder);
        }
        
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<Order> getRestaurantOrders(com.srFoodDelivery.model.Restaurant restaurant) {
        return orderRepository.findByRestaurantOrderByCreatedAtDesc(restaurant);
    }

    // Chef feature removed - getChefOrders method deprecated

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }
    
    /**
     * Convert Cart to MultiOrderCreateRequest for multi-restaurant orders
     */
    private MultiOrderCreateRequest convertCartToMultiOrderRequest(Cart cart, String deliveryAddress,
                                                                  String specialInstructions,
                                                                  BigDecimal discountAmount,
                                                                  String appliedCoupon,
                                                                  Long preorderSlotId,
                                                                  Long reservationId) {
        MultiOrderCreateRequest request = new MultiOrderCreateRequest();
        
        // Convert cart items to CartItemRequest list
        List<MultiOrderCreateRequest.CartItemRequest> cartItemRequests = cart.getItems().stream()
                .map(item -> {
                    MultiOrderCreateRequest.CartItemRequest cartItemRequest = 
                            new MultiOrderCreateRequest.CartItemRequest();
                    cartItemRequest.setMenuItemId(item.getMenuItem().getId());
                    cartItemRequest.setQuantity(item.getQuantity());
                    return cartItemRequest;
                })
                .collect(Collectors.toList());
        
        request.setCartItems(cartItemRequests);
        request.setDeliveryAddress(deliveryAddress);
        request.setSpecialInstructions(specialInstructions);
        request.setDiscountAmount(discountAmount);
        request.setAppliedCoupon(appliedCoupon);
        request.setPreorderSlotId(preorderSlotId);
        request.setReservationId(reservationId);
        
        return request;
    }
}

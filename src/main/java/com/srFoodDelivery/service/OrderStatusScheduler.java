package com.srFoodDelivery.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.Order;
import com.srFoodDelivery.model.OrderStatus;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.OrderRepository;

@Component
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;
    private final EmailService emailService;

    public OrderStatusScheduler(OrderRepository orderRepository, EmailService emailService) {
        this.orderRepository = orderRepository;
        this.emailService = emailService;
    }

    /**
     * Automatically update order statuses based on time elapsed
     * Runs every 10 seconds for demo purposes
     * Complete flow: NEW → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED (within 1 minute)
     */
    @Scheduled(fixedRate = 10000) // 10 seconds
    @Transactional
    public void autoUpdateOrderStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> activeOrders = orderRepository.findAll().stream()
                .filter(order -> !order.getStatus().equals(OrderStatus.DELIVERED) 
                        && !order.getStatus().equals(OrderStatus.CANCELLED))
                .toList();

        for (Order order : activeOrders) {
            LocalDateTime createdAt = order.getCreatedAt();
            long secondsElapsed = java.time.Duration.between(createdAt, now).getSeconds();

            String currentStatus = order.getStatus();
            String newStatus = null;

            // Auto-progress order status based on time (all within 1 minute for demo)
            if (currentStatus.equals(OrderStatus.NEW) && secondsElapsed >= 10) {
                newStatus = OrderStatus.CONFIRMED;
                order.setTrackingInfo("Order confirmed by restaurant/chef");
            } else if (currentStatus.equals(OrderStatus.CONFIRMED) && secondsElapsed >= 20) {
                newStatus = OrderStatus.PREPARING;
                order.setTrackingInfo("Your order is being prepared");
            } else if (currentStatus.equals(OrderStatus.PREPARING) && secondsElapsed >= 40) {
                newStatus = OrderStatus.OUT_FOR_DELIVERY;
                order.setTrackingInfo("Your order is out for delivery");
                // Set estimated delivery time (20 seconds from now for demo)
                order.setEstimatedDeliveryTime(now.plusSeconds(20));
            } else if (currentStatus.equals(OrderStatus.OUT_FOR_DELIVERY) && secondsElapsed >= 60) {
                newStatus = OrderStatus.DELIVERED;
                order.setTrackingInfo("Order delivered successfully");
                order.setActualDeliveryTime(now);
            }

            if (newStatus != null && !newStatus.equals(currentStatus)) {
                order.setStatus(newStatus);
                Order savedOrder = orderRepository.save(order);
                
                // Get the customer user from the order (ensure it's loaded)
                User customer = savedOrder.getUser();
                if (customer != null) {
                    // Send email notification for status change to the customer
                    try {
                        emailService.sendOrderStatusUpdateEmail(customer, savedOrder);
                    } catch (Exception e) {
                        // Log error but don't fail the update
                        System.err.println("Failed to send email for order " + savedOrder.getId() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}


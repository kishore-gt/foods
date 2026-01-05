package com.srFoodDelivery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.Restaurant;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.websocket.OrderWebSocketPublisher;

@Service
@Transactional
public class RestaurantNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantNotificationService.class);

    private final OrderWebSocketPublisher webSocketPublisher;
    private final NotificationService notificationService;

    public RestaurantNotificationService(
            OrderWebSocketPublisher webSocketPublisher,
            NotificationService notificationService) {
        this.webSocketPublisher = webSocketPublisher;
        this.notificationService = notificationService;
    }

    /**
     * Notifies restaurant about a new suborder
     */
    public void notifyNewSubOrder(SubOrder subOrder) {
        if (subOrder.getRestaurant() == null) {
            return; // Chef order, not restaurant
        }

        Restaurant restaurant = subOrder.getRestaurant();
        User owner = restaurant.getOwner();
        MultiOrder multiOrder = subOrder.getMultiOrder();

        // Send WebSocket notification
        webSocketPublisher.publishToRestaurant(
                restaurant.getId(),
                "NEW_SUBORDER",
                createSubOrderPayload(subOrder, multiOrder));

        // Create persistent notification
        notificationService.createNotification(
                owner,
                "New Order Received",
                String.format("You have received a new order #%d for ₹%.2f", 
                        subOrder.getId(), subOrder.getTotalAmount()),
                "NEW_ORDER",
                "SUB_ORDER",
                subOrder.getId());

        logger.info("Notified restaurant {} about new SubOrder {}", restaurant.getId(), subOrder.getId());
    }

    /**
     * Notifies restaurant about suborder status update
     */
    public void notifySubOrderStatusUpdate(SubOrder subOrder) {
        if (subOrder.getRestaurant() == null) {
            return;
        }

        Restaurant restaurant = subOrder.getRestaurant();
        User owner = restaurant.getOwner();

        // Send WebSocket notification
        webSocketPublisher.publishToRestaurant(
                restaurant.getId(),
                "SUBORDER_STATUS_UPDATE",
                createSubOrderPayload(subOrder, subOrder.getMultiOrder()));

        logger.info("Notified restaurant {} about SubOrder {} status update: {}", 
                restaurant.getId(), subOrder.getId(), subOrder.getStatus());
    }

    private Object createSubOrderPayload(SubOrder subOrder, MultiOrder multiOrder) {
        return new SubOrderPayload(
                subOrder.getId(),
                multiOrder.getId(),
                subOrder.getRestaurant() != null ? subOrder.getRestaurant().getId() : null,
                subOrder.getStatus(),
                subOrder.getTotalAmount(),
                subOrder.getTrackingInfo(),
                subOrder.getEstimatedDeliveryTime(),
                subOrder.getRider() != null ? subOrder.getRider().getId() : null
        );
    }

    private static class SubOrderPayload {
        private final Long subOrderId;
        private final Long multiOrderId;
        private final Long restaurantId;
        private final String status;
        private final java.math.BigDecimal totalAmount;
        private final String trackingInfo;
        private final java.time.LocalDateTime estimatedDeliveryTime;
        private final Long riderId;

        public SubOrderPayload(Long subOrderId, Long multiOrderId, Long restaurantId, String status,
                              java.math.BigDecimal totalAmount, String trackingInfo,
                              java.time.LocalDateTime estimatedDeliveryTime, Long riderId) {
            this.subOrderId = subOrderId;
            this.multiOrderId = multiOrderId;
            this.restaurantId = restaurantId;
            this.status = status;
            this.totalAmount = totalAmount;
            this.trackingInfo = trackingInfo;
            this.estimatedDeliveryTime = estimatedDeliveryTime;
            this.riderId = riderId;
        }

        // Getters
        public Long getSubOrderId() { return subOrderId; }
        public Long getMultiOrderId() { return multiOrderId; }
        public Long getRestaurantId() { return restaurantId; }
        public String getStatus() { return status; }
        public java.math.BigDecimal getTotalAmount() { return totalAmount; }
        public String getTrackingInfo() { return trackingInfo; }
        public java.time.LocalDateTime getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
        public Long getRiderId() { return riderId; }
    }
}


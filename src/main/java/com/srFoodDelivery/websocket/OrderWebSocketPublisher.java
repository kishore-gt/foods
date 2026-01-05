package com.srFoodDelivery.websocket;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderWebSocketPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderWebSocketPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public OrderWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast order event to user's personal topic
     */
    public void publishToUser(Long userId, String event, Object payload) {
        String destination = "/topic/user." + userId + ".orders";
        Map<String, Object> message = createMessage(event, payload);
        messagingTemplate.convertAndSend(destination, message);
        logger.debug("Published to {}: event={}", destination, event);
    }

    /**
     * Broadcast order event to restaurant topic
     */
    public void publishToRestaurant(Long restaurantId, String event, Object payload) {
        String destination = "/topic/restaurant." + restaurantId;
        Map<String, Object> message = createMessage(event, payload);
        messagingTemplate.convertAndSend(destination, message);
        logger.debug("Published to {}: event={}", destination, event);
    }

    /**
     * Broadcast order event to rider topic
     */
    public void publishToRider(Long riderId, String event, Object payload) {
        String destination = "/topic/rider." + riderId;
        Map<String, Object> message = createMessage(event, payload);
        messagingTemplate.convertAndSend(destination, message);
        logger.debug("Published to {}: event={}", destination, event);
    }

    /**
     * Broadcast rider location update for a specific multi-order
     */
    public void publishRiderLocation(Long multiOrderId, Long riderId, Double lat, Double lon) {
        String destination = "/topic/rider.locations." + multiOrderId;
        Map<String, Object> location = new HashMap<>();
        location.put("event", "RIDER_LOCATION_UPDATE");
        location.put("riderId", riderId);
        location.put("lat", lat);
        location.put("lon", lon);
        location.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend(destination, location);
        logger.debug("Published rider location to {}: riderId={}, lat={}, lon={}", 
                destination, riderId, lat, lon);
    }

    /**
     * Broadcast generic order update
     */
    public void publishOrderUpdate(Long multiOrderId, Long subOrderId, String status, 
                                   Long riderId, Double lat, Double lon) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("multiOrderId", multiOrderId);
        payload.put("subOrderId", subOrderId);
        payload.put("status", status);
        if (riderId != null) {
            payload.put("riderId", riderId);
        }
        if (lat != null && lon != null) {
            payload.put("lat", lat);
            payload.put("lon", lon);
        }
        payload.put("timestamp", System.currentTimeMillis());

        // Determine event type from status
        String event = "ORDER_STATUS_UPDATE";
        if ("CONFIRMED".equals(status)) {
            event = "ORDER_CONFIRMED";
        } else if ("PREPARING".equals(status)) {
            event = "ORDER_PREPARING";
        } else if ("OUT_FOR_DELIVERY".equals(status)) {
            event = "ORDER_OUT_FOR_DELIVERY";
        } else if ("DELIVERED".equals(status)) {
            event = "ORDER_DELIVERED";
        }

        Map<String, Object> message = createMessage(event, payload);
        
        // Publish to user topic (if we have multiOrderId, we can look up user)
        // Note: This requires MultiOrder lookup - for now, publish to general topic
        // Publish to restaurant topic (if we have subOrderId, we can look up restaurant)
        // Publish to rider topic (if riderId is provided)
        if (riderId != null) {
            publishToRider(riderId, event, payload);
        }
        
        // Publish to general orders topic (customers can subscribe to this)
        messagingTemplate.convertAndSend("/topic/orders", message);
        
        // Also publish to multi-order specific topic for real-time tracking
        messagingTemplate.convertAndSend("/topic/multiorder." + multiOrderId, message);
    }

    private Map<String, Object> createMessage(String event, Object payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("event", event);
        message.put("payload", payload);
        message.put("timestamp", System.currentTimeMillis());
        return message;
    }
}


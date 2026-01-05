package com.srFoodDelivery.websocket;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.srFoodDelivery.model.ChatMessage;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.MultiOrderRepository;
import com.srFoodDelivery.repository.SubOrderRepository;
import com.srFoodDelivery.repository.UserRepository;
import com.srFoodDelivery.service.chat.ChatService;

@Controller
public class ChatWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final MultiOrderRepository multiOrderRepository;
    private final SubOrderRepository subOrderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketHandler(
            ChatService chatService,
            UserRepository userRepository,
            MultiOrderRepository multiOrderRepository,
            SubOrderRepository subOrderRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.multiOrderRepository = multiOrderRepository;
        this.subOrderRepository = subOrderRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles incoming chat messages via WebSocket
     * Message format: { senderId, receiverId, message, orderId?, subOrderId? }
     */
    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public Map<String, Object> handleChatMessage(@Payload Map<String, Object> messageData) {
        try {
            Long senderId = Long.valueOf(messageData.get("senderId").toString());
            Long receiverId = Long.valueOf(messageData.get("receiverId").toString());
            String message = messageData.get("message").toString();
            
            Long orderId = messageData.containsKey("orderId") && messageData.get("orderId") != null ?
                    Long.valueOf(messageData.get("orderId").toString()) : null;
            Long subOrderId = messageData.containsKey("subOrderId") && messageData.get("subOrderId") != null ?
                    Long.valueOf(messageData.get("subOrderId").toString()) : null;

            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
            User receiver = userRepository.findById(receiverId)
                    .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

            // Get order and suborder if provided
            MultiOrder order = orderId != null ? 
                    multiOrderRepository.findById(orderId).orElse(null) : null;
            SubOrder subOrder = subOrderId != null ? 
                    subOrderRepository.findById(subOrderId).orElse(null) : null;

            ChatMessage chatMessage = chatService.sendMessage(sender, receiver, message, order, subOrder);

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("id", chatMessage.getId());
            response.put("senderId", senderId);
            response.put("receiverId", receiverId);
            response.put("message", message);
            response.put("timestamp", chatMessage.getCreatedAt().toString());
            response.put("orderId", orderId);
            response.put("subOrderId", subOrderId);

            // Send to specific receiver's topic
            messagingTemplate.convertAndSend("/topic/chat.user." + receiverId, response);
            
            // Also send to sender's topic for confirmation
            messagingTemplate.convertAndSend("/topic/chat.user." + senderId, response);

            logger.info("Chat message sent from user {} to user {}", senderId, receiverId);

            return response;
        } catch (Exception e) {
            logger.error("Error handling chat message", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to send message: " + e.getMessage());
            return error;
        }
    }
}


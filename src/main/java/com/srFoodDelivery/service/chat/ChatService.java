package com.srFoodDelivery.service.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.ChatMessage;
import com.srFoodDelivery.model.ChatRoom;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.ChatMessageRepository;
import com.srFoodDelivery.repository.ChatRoomRepository;

@Service
@Transactional
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            ChatRoomRepository chatRoomRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomRepository = chatRoomRepository;
    }

    /**
     * Sends a message between two users
     */
    public ChatMessage sendMessage(User sender, User receiver, String message, 
                                   MultiOrder order, SubOrder subOrder) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(sender);
        chatMessage.setReceiver(receiver);
        chatMessage.setMessage(message);
        chatMessage.setMessageType("TEXT");
        chatMessage.setOrder(order);
        chatMessage.setSubOrder(subOrder);
        chatMessage.setIsRead(false);

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        // Update or create chat room
        ChatRoom chatRoom = getOrCreateChatRoom(sender, receiver, order, subOrder);
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        logger.info("Message sent from user {} to user {} for order {}", 
                sender.getId(), receiver.getId(), order != null ? order.getId() : "N/A");

        return savedMessage;
    }

    /**
     * Gets or creates a chat room between two users
     */
    public ChatRoom getOrCreateChatRoom(User user1, User user2, MultiOrder order, SubOrder subOrder) {
        // Ensure consistent ordering (smaller ID first)
        User participant1 = user1.getId() < user2.getId() ? user1 : user2;
        User participant2 = user1.getId() < user2.getId() ? user2 : user1;

        Optional<ChatRoom> existingRoom;
        if (subOrder != null) {
            existingRoom = chatRoomRepository.findChatRoomForSubOrder(subOrder, participant1, participant2);
        } else if (order != null) {
            existingRoom = chatRoomRepository.findChatRoomForOrder(order, participant1, participant2);
        } else {
            // General chat room (no order context)
            existingRoom = chatRoomRepository.findChatRoomsForUser(participant1).stream()
                    .filter(room -> room.hasParticipant(participant2.getId()) && 
                            room.getOrder() == null && room.getSubOrder() == null)
                    .findFirst();
        }

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        // Create new chat room
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setParticipant1(participant1);
        chatRoom.setParticipant2(participant2);
        chatRoom.setOrder(order);
        chatRoom.setSubOrder(subOrder);
        chatRoom.setLastMessageAt(LocalDateTime.now());

        return chatRoomRepository.save(chatRoom);
    }

    /**
     * Gets conversation between two users
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getConversation(User user1, User user2, SubOrder subOrder) {
        if (subOrder != null) {
            return chatMessageRepository.findConversationForSubOrder(subOrder, user1, user2);
        }
        return chatMessageRepository.findConversationBetweenUsers(user1, user2);
    }

    /**
     * Gets conversation for an order
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getConversationForOrder(User user1, User user2, MultiOrder order) {
        return chatMessageRepository.findConversationForOrder(order, user1, user2);
    }

    /**
     * Gets all chat rooms for a user
     */
    @Transactional(readOnly = true)
    public List<ChatRoom> getChatRoomsForUser(User user) {
        return chatRoomRepository.findChatRoomsForUser(user);
    }

    /**
     * Marks messages as read
     */
    public void markMessagesAsRead(User user, List<Long> messageIds) {
        for (Long messageId : messageIds) {
            ChatMessage message = chatMessageRepository.findById(messageId)
                    .orElse(null);
            if (message != null && message.getReceiver().getId().equals(user.getId())) {
                message.setIsRead(true);
                chatMessageRepository.save(message);
            }
        }
    }

    /**
     * Gets unread message count for a user
     */
    @Transactional(readOnly = true)
    public Long getUnreadMessageCount(User user) {
        return chatMessageRepository.countUnreadMessages(user);
    }

    /**
     * Gets unread messages for a user
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getUnreadMessages(User user) {
        return chatMessageRepository.findUnreadMessages(user);
    }
}


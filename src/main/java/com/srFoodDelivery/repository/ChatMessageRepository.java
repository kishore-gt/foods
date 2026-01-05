package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.ChatMessage;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "((cm.sender = :user1 AND cm.receiver = :user2) OR " +
           "(cm.sender = :user2 AND cm.receiver = :user1)) " +
           "ORDER BY cm.createdAt ASC")
    List<ChatMessage> findConversationBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "cm.subOrder = :subOrder AND " +
           "((cm.sender = :user1 AND cm.receiver = :user2) OR " +
           "(cm.sender = :user2 AND cm.receiver = :user1)) " +
           "ORDER BY cm.createdAt ASC")
    List<ChatMessage> findConversationForSubOrder(
            @Param("subOrder") SubOrder subOrder,
            @Param("user1") User user1,
            @Param("user2") User user2);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "cm.order = :order AND " +
           "((cm.sender = :user1 AND cm.receiver = :user2) OR " +
           "(cm.sender = :user2 AND cm.receiver = :user1)) " +
           "ORDER BY cm.createdAt ASC")
    List<ChatMessage> findConversationForOrder(
            @Param("order") MultiOrder order,
            @Param("user1") User user1,
            @Param("user2") User user2);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.receiver = :user AND cm.isRead = false")
    Long countUnreadMessages(@Param("user") User user);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.receiver = :user AND cm.isRead = false ORDER BY cm.createdAt DESC")
    List<ChatMessage> findUnreadMessages(@Param("user") User user);
}


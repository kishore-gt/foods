package com.srFoodDelivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.ChatRoom;
import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.SubOrder;
import com.srFoodDelivery.model.User;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    @Query("SELECT cr FROM ChatRoom cr WHERE " +
           "((cr.participant1 = :user1 AND cr.participant2 = :user2) OR " +
           "(cr.participant1 = :user2 AND cr.participant2 = :user1)) " +
           "AND cr.subOrder = :subOrder")
    Optional<ChatRoom> findChatRoomForSubOrder(
            @Param("subOrder") SubOrder subOrder,
            @Param("user1") User user1,
            @Param("user2") User user2);
    
    @Query("SELECT cr FROM ChatRoom cr WHERE " +
           "((cr.participant1 = :user1 AND cr.participant2 = :user2) OR " +
           "(cr.participant1 = :user2 AND cr.participant2 = :user1)) " +
           "AND cr.order = :order")
    Optional<ChatRoom> findChatRoomForOrder(
            @Param("order") MultiOrder order,
            @Param("user1") User user1,
            @Param("user2") User user2);
    
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.participant1 = :user OR cr.participant2 = :user ORDER BY cr.lastMessageAt DESC NULLS LAST, cr.updatedAt DESC")
    List<ChatRoom> findChatRoomsForUser(@Param("user") User user);
}


package com.srFoodDelivery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.srFoodDelivery.model.NotificationEntity;
import com.srFoodDelivery.model.User;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    
    List<NotificationEntity> findByUserOrderByCreatedAtDesc(User user);
    
    List<NotificationEntity> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);
    
    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.user = :user AND n.isRead = false")
    long countUnreadByUser(@Param("user") User user);
    
    @Query("SELECT n FROM NotificationEntity n WHERE n.user = :user " +
           "AND n.relatedEntityType = :entityType AND n.relatedEntityId = :entityId " +
           "ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByUserAndRelatedEntity(
        @Param("user") User user,
        @Param("entityType") String entityType,
        @Param("entityId") Long entityId
    );
}


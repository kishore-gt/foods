package com.srFoodDelivery.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srFoodDelivery.model.NotificationEntity;
import com.srFoodDelivery.model.User;
import com.srFoodDelivery.repository.NotificationRepository;
import com.srFoodDelivery.websocket.OrderWebSocketPublisher;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final OrderWebSocketPublisher webSocketPublisher;

    public NotificationService(
            NotificationRepository notificationRepository,
            OrderWebSocketPublisher webSocketPublisher) {
        this.notificationRepository = notificationRepository;
        this.webSocketPublisher = webSocketPublisher;
    }

    /**
     * Creates and sends a notification to a user
     */
    public NotificationEntity createNotification(
            User user,
            String title,
            String message,
            String notificationType,
            String relatedEntityType,
            Long relatedEntityId) {
        
        NotificationEntity notification = new NotificationEntity();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setIsRead(false);

        NotificationEntity saved = notificationRepository.save(notification);
        
        // Send via WebSocket
        webSocketPublisher.publishToUser(user.getId(), "NOTIFICATION", createNotificationPayload(saved));
        
        logger.info("Created notification for user {}: {}", user.getId(), title);
        
        return saved;
    }

    /**
     * Marks a notification as read
     */
    public void markAsRead(Long notificationId, User user) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Access denied");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Gets all notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationEntity> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Gets unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationEntity> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    /**
     * Gets unread notification count for a user
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countUnreadByUser(user);
    }

    private Object createNotificationPayload(NotificationEntity notification) {
        return new NotificationPayload(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getNotificationType(),
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }

    private static class NotificationPayload {
        private final Long id;
        private final String title;
        private final String message;
        private final String notificationType;
        private final String relatedEntityType;
        private final Long relatedEntityId;
        private final Boolean isRead;
        private final java.time.LocalDateTime createdAt;

        public NotificationPayload(Long id, String title, String message, String notificationType,
                                  String relatedEntityType, Long relatedEntityId, Boolean isRead,
                                  java.time.LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.notificationType = notificationType;
            this.relatedEntityType = relatedEntityType;
            this.relatedEntityId = relatedEntityId;
            this.isRead = isRead;
            this.createdAt = createdAt;
        }

        // Getters
        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getNotificationType() { return notificationType; }
        public String getRelatedEntityType() { return relatedEntityType; }
        public Long getRelatedEntityId() { return relatedEntityId; }
        public Boolean getIsRead() { return isRead; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    }
}


package com.srFoodDelivery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.srFoodDelivery.model.MultiOrder;
import com.srFoodDelivery.model.Order;
import com.srFoodDelivery.model.OrderStatus;
import com.srFoodDelivery.model.User;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        if (mailSender == null) {
            logger.error("JavaMailSender is NULL! Email functionality will not work. Check Spring Boot mail autoconfiguration.");
        } else {
            logger.info("EmailService initialized successfully with JavaMailSender");
        }
    }

    public void sendOrderConfirmationEmail(User user, Order order) {
        if (mailSender == null) {
            logger.warn("JavaMailSender is not configured. Email will not be sent.");
            return;
        }
        
        try {
            logger.info("Sending order confirmation email to {} for order #{}", user.getEmail(), order.getId());
            
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                logger.warn("User email is empty. Cannot send email.");
                return;
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            String senderEmail = fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "noreply@tummygo.com";
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject("Order Confirmation - Tummy Go!");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "Your order #%d has been confirmed.\n\n" +
                "Order Details:\n" +
                "Total Amount: ₹%.2f\n" +
                "Delivery Address: %s\n" +
                "Status: %s\n\n" +
                "Thank you for your order!\n\n" +
                "Best regards,\n" +
                "Tummy Go! Team",
                user.getFullName() != null ? user.getFullName() : "Customer",
                order.getId(),
                order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0,
                order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "N/A",
                order.getStatus() != null ? order.getStatus() : "CONFIRMED"
            ));
            
            mailSender.send(message);
            logger.info("✅ EMAIL SENT SUCCESSFULLY to {}", user.getEmail());
            logger.info("Email Subject: Order Confirmation - Tummy Go!");
        } catch (org.springframework.mail.MailAuthenticationException e) {
            logger.error("Email authentication failed. Please check your email credentials in application.properties: {}", e.getMessage());
        } catch (org.springframework.mail.MailSendException e) {
            logger.error("Failed to send email to {}. Check SMTP settings and network connection: {}", user.getEmail(), e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to send order confirmation email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    public void sendOrderStatusUpdateEmail(User user, Order order) {
        if (mailSender == null) {
            logger.warn("JavaMailSender is not configured. Email will not be sent.");
            return;
        }
        
        try {
            logger.info("Sending order status update email to {} for order #{}", user.getEmail(), order.getId());
            
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                logger.warn("User email is empty. Cannot send email.");
                return;
            }
            
            String statusMessage = getStatusMessage(order.getStatus());
            
            SimpleMailMessage message = new SimpleMailMessage();
            String senderEmail = fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "noreply@tummygo.com";
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject("Order Status Update - Tummy Go!");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "Your order #%d status has been updated.\n\n" +
                "New Status: %s\n" +
                "Order Total: ₹%.2f\n" +
                "Delivery Address: %s\n\n" +
                "Thank you!\n\n" +
                "Best regards,\n" +
                "Tummy Go! Team",
                user.getFullName() != null ? user.getFullName() : "Customer",
                order.getId(),
                statusMessage,
                order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0,
                order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "N/A"
            ));
            
            mailSender.send(message);
            logger.info("Order status update email sent successfully to {}", user.getEmail());
        } catch (org.springframework.mail.MailAuthenticationException e) {
            logger.error("Email authentication failed. Please check your email credentials in application.properties: {}", e.getMessage());
        } catch (org.springframework.mail.MailSendException e) {
            logger.error("Failed to send email to {}. Check SMTP settings and network connection: {}", user.getEmail(), e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to send order status update email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    public void sendMultiOrderConfirmationEmail(User user, MultiOrder multiOrder) {
        if (mailSender == null) {
            logger.error("JavaMailSender is NULL. Email will not be sent. Check application.properties email configuration.");
            return;
        }
        
        try {
            logger.info("=== ATTEMPTING TO SEND MULTIORDER CONFIRMATION EMAIL ===");
            logger.info("To: {}", user != null ? user.getEmail() : "NULL USER");
            logger.info("MultiOrder ID: {}", multiOrder != null ? multiOrder.getId() : "NULL ORDER");
            
            if (user == null) {
                logger.error("User is NULL. Cannot send email.");
                return;
            }
            
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                logger.error("User email is empty. Cannot send email. User: {}", user.getFullName());
                return;
            }
            
            if (multiOrder == null) {
                logger.error("MultiOrder is NULL. Cannot send email.");
                return;
            }
            
            // Calculate total amount from sub-orders
            double totalAmount = multiOrder.getSubOrders().stream()
                .mapToDouble(so -> so.getTotalAmount() != null ? so.getTotalAmount().doubleValue() : 0.0)
                .sum();
            
            // Build order details
            StringBuilder orderDetails = new StringBuilder();
            orderDetails.append("Order #").append(multiOrder.getId()).append("\n\n");
            orderDetails.append("Order Details:\n");
            
            for (var subOrder : multiOrder.getSubOrders()) {
                if (subOrder.getRestaurant() != null) {
                    orderDetails.append("- ").append(subOrder.getRestaurant().getName());
                    if (subOrder.getTotalAmount() != null) {
                        orderDetails.append(": ₹").append(String.format("%.2f", subOrder.getTotalAmount().doubleValue()));
                    }
                    orderDetails.append("\n");
                }
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            String senderEmail = fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "noreply@tummygo.com";
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject("Order Confirmation - Tummy Go!");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "Your order has been confirmed!\n\n" +
                "%s" +
                "Total Amount: ₹%.2f\n" +
                "Delivery Address: %s\n" +
                "Order Status: %s\n" +
                "Payment Status: %s\n\n" +
                "Thank you for your order!\n\n" +
                "Best regards,\n" +
                "Tummy Go! Team",
                user.getFullName() != null ? user.getFullName() : "Customer",
                orderDetails.toString(),
                totalAmount,
                multiOrder.getDeliveryAddress() != null ? multiOrder.getDeliveryAddress() : "N/A",
                multiOrder.getStatus() != null ? multiOrder.getStatus() : "CONFIRMED",
                multiOrder.getPaymentStatus() != null ? multiOrder.getPaymentStatus() : "PENDING"
            ));
            
            mailSender.send(message);
            logger.info("✅ EMAIL SENT SUCCESSFULLY to {}", user.getEmail());
            logger.info("Email Subject: Order Confirmation - Tummy Go!");
        } catch (org.springframework.mail.MailAuthenticationException e) {
            logger.error("Email authentication failed. Please check your email credentials in application.properties: {}", e.getMessage());
        } catch (org.springframework.mail.MailSendException e) {
            logger.error("Failed to send email to {}. Check SMTP settings and network connection: {}", user.getEmail(), e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to send MultiOrder confirmation email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    public void sendMultiOrderStatusUpdateEmail(User user, MultiOrder multiOrder) {
        if (mailSender == null) {
            logger.warn("JavaMailSender is not configured. Email will not be sent.");
            return;
        }
        
        try {
            logger.info("Sending MultiOrder status update email to {} for MultiOrder #{}", user.getEmail(), multiOrder.getId());
            
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                logger.warn("User email is empty. Cannot send email.");
                return;
            }
            
            String statusMessage = getStatusMessage(multiOrder.getStatus());
            
            // Calculate total amount from sub-orders
            double totalAmount = multiOrder.getSubOrders().stream()
                .mapToDouble(so -> so.getTotalAmount() != null ? so.getTotalAmount().doubleValue() : 0.0)
                .sum();
            
            SimpleMailMessage message = new SimpleMailMessage();
            String senderEmail = fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "noreply@tummygo.com";
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject("Order Status Update - Tummy Go!");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "Your order #%d status has been updated.\n\n" +
                "New Status: %s\n" +
                "Order Total: ₹%.2f\n" +
                "Delivery Address: %s\n\n" +
                "Thank you!\n\n" +
                "Best regards,\n" +
                "Tummy Go! Team",
                user.getFullName() != null ? user.getFullName() : "Customer",
                multiOrder.getId(),
                statusMessage,
                totalAmount,
                multiOrder.getDeliveryAddress() != null ? multiOrder.getDeliveryAddress() : "N/A"
            ));
            
            mailSender.send(message);
            logger.info("✅ EMAIL SENT SUCCESSFULLY to {}", user.getEmail());
            logger.info("Email Subject: Order Status Update - Tummy Go!");
        } catch (org.springframework.mail.MailAuthenticationException e) {
            logger.error("Email authentication failed. Please check your email credentials in application.properties: {}", e.getMessage());
        } catch (org.springframework.mail.MailSendException e) {
            logger.error("Failed to send email to {}. Check SMTP settings and network connection: {}", user.getEmail(), e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to send MultiOrder status update email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    public void sendPreorderApprovalEmail(User user, MultiOrder multiOrder, String restaurantName) {
        if (mailSender == null) {
            logger.error("JavaMailSender is NULL. Email will not be sent. Check application.properties email configuration.");
            return;
        }
        
        try {
            logger.info("=== ATTEMPTING TO SEND PREORDER APPROVAL EMAIL ===");
            logger.info("To: {}", user != null ? user.getEmail() : "NULL USER");
            logger.info("MultiOrder ID: {}", multiOrder != null ? multiOrder.getId() : "NULL ORDER");
            
            if (user == null) {
                logger.error("User is NULL. Cannot send email.");
                return;
            }
            
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                logger.error("User email is empty. Cannot send email. User: {}", user.getFullName());
                return;
            }
            
            if (multiOrder == null) {
                logger.error("MultiOrder is NULL. Cannot send email.");
                return;
            }
            
            // Calculate total amount from sub-orders
            double totalAmount = multiOrder.getSubOrders().stream()
                .mapToDouble(so -> so.getTotalAmount() != null ? so.getTotalAmount().doubleValue() : 0.0)
                .sum();
            
            SimpleMailMessage message = new SimpleMailMessage();
            String senderEmail = fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "noreply@tummygo.com";
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject("Preorder Approved - Tummy Go!");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "Great news! Your preorder #%d has been approved by %s.\n\n" +
                "Order Details:\n" +
                "Restaurant: %s\n" +
                "Total Amount: ₹%.2f\n" +
                "Delivery Address: %s\n" +
                "Status: CONFIRMED\n\n" +
                "Your order is now confirmed and will be prepared as scheduled.\n\n" +
                "Thank you for choosing Tummy Go!!\n\n" +
                "Best regards,\n" +
                "Tummy Go! Team",
                user.getFullName() != null ? user.getFullName() : "Customer",
                multiOrder.getId(),
                restaurantName,
                restaurantName,
                totalAmount,
                multiOrder.getDeliveryAddress() != null ? multiOrder.getDeliveryAddress() : "N/A"
            ));
            
            mailSender.send(message);
            logger.info("✅ EMAIL SENT SUCCESSFULLY to {}", user.getEmail());
            logger.info("Email Subject: Preorder Approved - Tummy Go!");
        } catch (org.springframework.mail.MailAuthenticationException e) {
            logger.error("Email authentication failed. Please check your email credentials in application.properties: {}", e.getMessage());
        } catch (org.springframework.mail.MailSendException e) {
            logger.error("Failed to send email to {}. Check SMTP settings and network connection: {}", user.getEmail(), e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to send preorder approval email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    public void sendPreorderRejectionEmail(User user, MultiOrder multiOrder, String restaurantName, String reason) {
        if (mailSender == null) {
            logger.error("JavaMailSender is NULL. Email will not be sent. Check application.properties email configuration.");
            return;
        }
        
        try {
            logger.info("=== ATTEMPTING TO SEND PREORDER REJECTION EMAIL ===");
            logger.info("To: {}", user != null ? user.getEmail() : "NULL USER");
            logger.info("MultiOrder ID: {}", multiOrder != null ? multiOrder.getId() : "NULL ORDER");
            
            if (user == null) {
                logger.error("User is NULL. Cannot send email.");
                return;
            }
            
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                logger.error("User email is empty. Cannot send email. User: {}", user.getFullName());
                return;
            }
            
            if (multiOrder == null) {
                logger.error("MultiOrder is NULL. Cannot send email.");
                return;
            }
            
            // Calculate total amount from sub-orders
            double totalAmount = multiOrder.getSubOrders().stream()
                .mapToDouble(so -> so.getTotalAmount() != null ? so.getTotalAmount().doubleValue() : 0.0)
                .sum();
            
            SimpleMailMessage message = new SimpleMailMessage();
            String senderEmail = fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "noreply@tummygo.com";
            message.setFrom(senderEmail);
            message.setTo(user.getEmail());
            message.setSubject("Preorder Rejected - Tummy Go!");
            message.setText(String.format(
                "Dear %s,\n\n" +
                "We regret to inform you that your preorder #%d has been rejected by %s.\n\n" +
                "Order Details:\n" +
                "Restaurant: %s\n" +
                "Total Amount: ₹%.2f\n" +
                "Delivery Address: %s\n" +
                "Status: REJECTED\n" +
                "%s\n\n" +
                "If you have any questions or concerns, please contact the restaurant directly.\n\n" +
                "We apologize for any inconvenience.\n\n" +
                "Best regards,\n" +
                "Tummy Go! Team",
                user.getFullName() != null ? user.getFullName() : "Customer",
                multiOrder.getId(),
                restaurantName,
                restaurantName,
                totalAmount,
                multiOrder.getDeliveryAddress() != null ? multiOrder.getDeliveryAddress() : "N/A",
                reason != null && !reason.isEmpty() ? "Reason: " + reason + "\n" : ""
            ));
            
            mailSender.send(message);
            logger.info("✅ EMAIL SENT SUCCESSFULLY to {}", user.getEmail());
            logger.info("Email Subject: Preorder Rejected - Tummy Go!");
        } catch (org.springframework.mail.MailAuthenticationException e) {
            logger.error("Email authentication failed. Please check your email credentials in application.properties: {}", e.getMessage());
        } catch (org.springframework.mail.MailSendException e) {
            logger.error("Failed to send email to {}. Check SMTP settings and network connection: {}", user.getEmail(), e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to send preorder rejection email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    private String getStatusMessage(String status) {
        return switch (status) {
            case OrderStatus.CONFIRMED -> "Confirmed - Your order has been confirmed by the restaurant";
            case OrderStatus.PREPARING -> "Preparing - Your order is being prepared";
            case OrderStatus.OUT_FOR_DELIVERY -> "Out for Delivery - Your order is on the way";
            case OrderStatus.DELIVERED -> "Delivered - Your order has been delivered";
            case OrderStatus.CANCELLED -> "Cancelled - Your order has been cancelled";
            case "PENDING_APPROVAL" -> "Pending Approval - Your preorder is awaiting restaurant approval";
            case "REJECTED" -> "Rejected - Your preorder has been rejected";
            default -> status;
        };
    }
}


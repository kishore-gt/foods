# Email Notifications to Customers - Summary

## Overview
The application sends emails to customers at specific points in the order lifecycle. All email functionality is handled by the `EmailService` class.

## When Emails Are Sent

### 1. **Order Confirmation Email**
**When:** Immediately after a customer places an order and payment is completed

**Location:** `OrderService.placeOrder()` method (line 80)

**Trigger:** 
- Customer completes payment
- Order is successfully created
- Cart is cleared

**Email Details:**
- **Subject:** "Order Confirmation - Tummy Go!"
- **Content:** 
  - Customer name
  - Order ID
  - Total amount
  - Delivery address
  - Order status

**Code Reference:**
```java
// src/main/java/com/srFoodDelivery/service/OrderService.java
emailService.sendOrderConfirmationEmail(user, dummyOrder);
```

---

### 2. **Order Status Update Email**
**When:** Whenever the order status changes

**Locations:**
1. **Manual Status Update** - `OrderService.updateStatus()` method (line 100)
   - When restaurant owner/rider manually updates order status
   - When order status is changed through admin panel

2. **Automatic Status Update** - `OrderStatusScheduler.autoUpdateOrderStatuses()` method (line 74)
   - Runs every 10 seconds (for demo purposes)
   - Automatically progresses orders through statuses:
     - NEW → CONFIRMED (after 10 seconds)
     - CONFIRMED → PREPARING (after 20 seconds)
     - PREPARING → OUT_FOR_DELIVERY (after 40 seconds)
     - OUT_FOR_DELIVERY → DELIVERED (after 60 seconds)

**Email Details:**
- **Subject:** "Order Status Update - Tummy Go!"
- **Content:**
  - Customer name
  - Order ID
  - New status with friendly message
  - Order total
  - Delivery address

**Status Messages:**
- **CONFIRMED:** "Confirmed - Your order has been confirmed by the restaurant"
- **PREPARING:** "Preparing - Your order is being prepared"
- **OUT_FOR_DELIVERY:** "Out for Delivery - Your order is on the way"
- **DELIVERED:** "Delivered - Your order has been delivered"
- **CANCELLED:** "Cancelled - Your order has been cancelled"

**Code References:**
```java
// Manual update
// src/main/java/com/srFoodDelivery/service/OrderService.java
emailService.sendOrderStatusUpdateEmail(customer, savedOrder);

// Automatic update
// src/main/java/com/srFoodDelivery/service/OrderStatusScheduler.java
emailService.sendOrderStatusUpdateEmail(customer, savedOrder);
```

---

## Email Service Configuration

**File:** `src/main/java/com/srFoodDelivery/service/EmailService.java`

**Features:**
- Uses Spring's `JavaMailSender` for sending emails
- Logs all email sending attempts (success and failures)
- Gracefully handles errors (doesn't crash the application if email fails)
- Default sender: `noreply@tummygo.com` (if not configured)

**Configuration Required:**
- Set `spring.mail.username` in `application.properties`
- Set `spring.mail.password` in `application.properties`
- Configure SMTP settings (see `EMAIL_SMS_SETUP.md` for details)

---

## Email Flow Diagram

```
Customer Places Order
        ↓
Payment Completed
        ↓
Order Created
        ↓
┌───────────────────────┐
│ Order Confirmation    │ ← Email #1 Sent
│ Email Sent            │
└───────────────────────┘
        ↓
Order Status Changes
        ↓
┌───────────────────────┐
│ Status Update Email   │ ← Email #2, #3, #4, #5 Sent
│ Sent (Each Change)    │
└───────────────────────┘
        ↓
Order Delivered
```

---

## Important Notes

1. **Error Handling:** All email sending is wrapped in try-catch blocks. If email fails, the order process continues without interruption.

2. **Logging:** All email operations are logged:
   - Success: `INFO` level
   - Failures: `ERROR` level

3. **Automatic Updates:** The scheduler runs every 10 seconds and automatically updates order statuses. This is for demo purposes and can be adjusted.

4. **Email Configuration:** Emails will only be sent if email configuration is properly set up in `application.properties`. Without configuration, email attempts will fail silently (logged but not blocking).

---

## Testing Email Functionality

1. **Check Configuration:**
   - Verify `application.properties` has email settings
   - See `EMAIL_SMS_SETUP.md` for setup instructions

2. **Test Order Confirmation:**
   - Place an order as a customer
   - Check application logs for email sending confirmation
   - Verify email received in customer's inbox

3. **Test Status Updates:**
   - Wait for automatic status updates (scheduler runs every 10 seconds)
   - Or manually update order status through owner/rider panel
   - Check logs and customer inbox for status update emails

---

## Files Involved

- `src/main/java/com/srFoodDelivery/service/EmailService.java` - Email service implementation
- `src/main/java/com/srFoodDelivery/service/OrderService.java` - Order confirmation email trigger
- `src/main/java/com/srFoodDelivery/service/OrderStatusScheduler.java` - Automatic status update emails
- `src/main/resources/application.properties` - Email configuration
- `EMAIL_SMS_SETUP.md` - Email setup guide


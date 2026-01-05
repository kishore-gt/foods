# Customer Email Notifications - Complete List

## Overview
Customers now receive email notifications for **EVERY action** related to their orders. All emails are sent automatically when actions occur.

## 📧 Email Notifications by Action

### 1. **Order Placement & Confirmation**
**When:** After customer places an order and payment is completed
**Location:** `PaymentController.finalizeOrder()`
**Email Method:** `EmailService.sendMultiOrderConfirmationEmail()`
**Email Subject:** "Order Confirmation - Tummy Go!"
**Content:**
- Customer name
- Order ID
- Restaurant names and amounts
- Total amount
- Delivery address
- Order status
- Payment status

---

### 2. **Preorder Placement & Confirmation**
**When:** After customer places a preorder and payment is completed
**Location:** `CustomerController.confirmPreorderPayment()`
**Email Method:** `EmailService.sendMultiOrderConfirmationEmail()`
**Email Subject:** "Order Confirmation - Tummy Go!"
**Content:**
- Customer name
- Order ID
- Restaurant names and amounts
- Total amount
- Delivery address
- Order status (PENDING_APPROVAL)
- Payment status

---

### 3. **Preorder Approval**
**When:** Restaurant owner approves a preorder
**Location:** `OwnerController.approvePreorder()`
**Email Method:** `EmailService.sendPreorderApprovalEmail()`
**Email Subject:** "Preorder Approved - Tummy Go!"
**Content:**
- Customer name
- Order ID
- Restaurant name
- Total amount
- Delivery address
- Confirmation message

---

### 4. **Preorder Rejection**
**When:** Restaurant owner rejects a preorder
**Location:** `OwnerController.rejectPreorder()`
**Email Method:** `EmailService.sendPreorderRejectionEmail()`
**Email Subject:** "Preorder Rejected - Tummy Go!"
**Content:**
- Customer name
- Order ID
- Restaurant name
- Total amount
- Delivery address
- Rejection reason (if provided)

---

### 5. **Order Status Update - PREPARING**
**When:** Restaurant owner updates order status to "Preparing Food"
**Location:** `OwnerController.updateOrderStatus()`
**Email Method:** `EmailService.sendMultiOrderStatusUpdateEmail()`
**Email Subject:** "Order Status Update - Tummy Go!"
**Content:**
- Customer name
- Order ID
- New status: "Preparing - Your order is being prepared"
- Order total
- Delivery address

---

### 6. **Order Status Update - OUT FOR DELIVERY**
**When:** Restaurant owner updates order status to "Out for Delivery"
**Location:** `OwnerController.updateOrderStatus()`
**Email Method:** `EmailService.sendMultiOrderStatusUpdateEmail()`
**Email Subject:** "Order Status Update - Tummy Go!"
**Content:**
- Customer name
- Order ID
- New status: "Out for Delivery - Your order is on the way"
- Order total
- Delivery address

---

### 7. **Order Delivered**
**When:** Rider marks order as "Delivered"
**Location:** `RiderController.updateOrderStatus()`
**Email Method:** `EmailService.sendMultiOrderStatusUpdateEmail()`
**Email Subject:** "Order Status Update - Tummy Go!"
**Content:**
- Customer name
- Order ID
- New status: "Delivered - Your order has been delivered"
- Order total
- Delivery address

---

### 8. **Automatic Status Updates** (Scheduler)
**When:** Order status automatically changes via scheduler
**Location:** `OrderStatusScheduler.autoUpdateOrderStatuses()`
**Email Method:** `EmailService.sendOrderStatusUpdateEmail()`
**Note:** This uses the old Order system, but emails are still sent

---

## 📋 Complete Email Flow

```
Customer Places Order
        ↓
Payment Completed
        ↓
┌─────────────────────────────┐
│ Order Confirmation Email    │ ← Email #1
│ (sendMultiOrderConfirmation)│
└─────────────────────────────┘
        ↓
For Preorders:
        ↓
Owner Approves/Rejects
        ↓
┌─────────────────────────────┐
│ Preorder Approval/Rejection │ ← Email #2
│ Email                       │
└─────────────────────────────┘
        ↓
For Delivery Orders:
        ↓
Owner Updates Status
        ↓
┌─────────────────────────────┐
│ Status Update Email         │ ← Email #2, #3
│ (PREPARING, OUT_FOR_DELIVERY)│
└─────────────────────────────┘
        ↓
Rider Marks Delivered
        ↓
┌─────────────────────────────┐
│ Status Update Email         │ ← Email #4
│ (DELIVERED)                 │
└─────────────────────────────┘
```

---

## ✅ All Customer Actions Covered

1. ✅ **Order Placement** - Email sent
2. ✅ **Preorder Placement** - Email sent
3. ✅ **Preorder Approval** - Email sent
4. ✅ **Preorder Rejection** - Email sent
5. ✅ **Status: Preparing** - Email sent
6. ✅ **Status: Out for Delivery** - Email sent
7. ✅ **Status: Delivered** - Email sent
8. ✅ **Automatic Status Updates** - Email sent

---

## 🔧 Technical Implementation

### Email Service Methods:
- `sendMultiOrderConfirmationEmail()` - For order/preorder confirmation
- `sendMultiOrderStatusUpdateEmail()` - For status changes
- `sendPreorderApprovalEmail()` - For preorder approval
- `sendPreorderRejectionEmail()` - For preorder rejection

### Controllers Updated:
- ✅ `PaymentController` - Order confirmation email
- ✅ `CustomerController` - Preorder confirmation email
- ✅ `OwnerController` - Preorder approval/rejection emails, status update emails
- ✅ `RiderController` - Delivery confirmation email

---

## 📝 Email Configuration

**SMTP Settings:** Configured in `application.properties`
- Host: smtp.gmail.com
- Port: 587
- Username: kishoremovva11@gmail.com
- Password: [Gmail App Password]

**Important:** Ensure Gmail App Password is correct for emails to be sent.

---

## 🎯 Summary

**Every customer action now triggers an email notification:**
- ✅ Order placed → Confirmation email
- ✅ Preorder placed → Confirmation email
- ✅ Preorder approved → Approval email
- ✅ Preorder rejected → Rejection email
- ✅ Status changed → Status update email
- ✅ Order delivered → Delivery confirmation email

All emails are sent automatically and include comprehensive order details.


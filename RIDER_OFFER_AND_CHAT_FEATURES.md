# Rider Offer System and Chat Support - Implementation Summary

## ✅ Features Implemented

### 1. Rider Offer System (Approve/Reject Before Assignment)

**Problem Solved:** Previously, orders were automatically assigned to riders. Now, the nearest rider receives an **offer** and must approve or reject it before assignment. Tracking only starts after rider approval.

#### Key Changes:

1. **New Entity: `RiderOffer`**
   - Tracks pending offers to riders
   - Status: PENDING, ACCEPTED, REJECTED, EXPIRED
   - Expires after 5 minutes if not responded to
   - Database table: `rider_offer`

2. **Updated Flow:**
   ```
   Order Created → Find Nearest Rider → Send Offer (Status: OFFERED)
   → Rider Approves → Assign Rider (Status: ACCEPTED) → Start Tracking
   → Rider Rejects → Find Next Nearest Rider → Send New Offer
   ```

3. **Updated Services:**
   - `RiderService.sendOfferToNearestRider()` - Sends offer instead of direct assignment
   - `RiderService.handleRiderOfferResponse()` - Handles approve/reject with offer ID
   - `DemoPaymentService` - Updated to use offer-based flow

4. **Status Flow:**
   - `PENDING` → Order created, waiting for rider assignment
   - `OFFERED` → Offer sent to nearest rider (NEW STATUS)
   - `ACCEPTED` → Rider approved, tracking starts
   - `EN_ROUTE`, `PICKED_UP`, `DELIVERED`, `COMPLETED` → Normal delivery flow

5. **WebSocket Notifications:**
   - Riders receive `ORDER_OFFER` notifications when offers are sent
   - Riders receive `ORDER_ACCEPTED` notifications when they accept

#### API Endpoints:

- `POST /rider/offers/{offerId}/accept` - Accept an offer
- `POST /rider/offers/{offerId}/reject` - Reject an offer
- Legacy endpoints still work for backward compatibility

---

### 2. Chat Support System

**Problem Solved:** Added real-time chat functionality between customers, riders, and restaurant owners.

#### Key Components:

1. **Entities:**
   - `ChatMessage` - Stores individual messages
   - `ChatRoom` - Represents a conversation between two users
   - Can be linked to orders/suborders for order-specific chats

2. **Services:**
   - `ChatService` - Handles message sending, room management, unread counts
   - `ChatWebSocketHandler` - Real-time WebSocket support for instant messaging

3. **Features:**
   - Order-specific chats (linked to MultiOrder or SubOrder)
   - General chats (not linked to any order)
   - Unread message tracking
   - Real-time message delivery via WebSocket

4. **Database Tables:**
   - `chat_message` - Stores all messages
   - `chat_room` - Tracks conversations

#### API Endpoints:

- `GET /chat` - List all chat rooms for user
- `GET /chat/order/{orderId}` - Chat for a specific order
- `GET /chat/suborder/{subOrderId}` - Chat for a specific suborder
- `POST /chat/send` - Send a message

#### WebSocket Topics:

- `/topic/chat.user.{userId}` - User-specific chat notifications
- `/app/chat.send` - Send message via WebSocket

---

## 📋 Database Migration

**File:** `src/main/resources/db/migration/V4__rider_offers_and_chat.sql`

### Tables Created:

1. **rider_offer**
   - Tracks pending offers to riders
   - Links suborders to riders with status tracking

2. **chat_message**
   - Stores chat messages
   - Links to users, orders, and suborders

3. **chat_room**
   - Represents conversations
   - Links participants and optional order context

---

## 🔄 Updated Components

### Services:
- ✅ `RiderService` - Offer-based assignment
- ✅ `DemoPaymentService` - Uses offer flow
- ✅ `ChatService` - New chat functionality

### Controllers:
- ✅ `RiderController` - Updated to handle offers
- ✅ `ChatController` - New chat endpoints

### WebSocket:
- ✅ `ChatWebSocketHandler` - Real-time chat support
- ✅ `OrderWebSocketPublisher` - Enhanced for offer notifications

### Repositories:
- ✅ `RiderOfferRepository` - Offer queries
- ✅ `ChatMessageRepository` - Message queries
- ✅ `ChatRoomRepository` - Room queries

---

## 🎯 Usage Examples

### Rider Accepting an Offer:

```java
// Rider receives offer notification via WebSocket
// Rider clicks "Accept" button
POST /rider/offers/{offerId}/accept

// System:
// 1. Marks offer as ACCEPTED
// 2. Assigns rider to suborder
// 3. Changes suborder status to ACCEPTED
// 4. Starts tracking
// 5. Sends notification to customer
```

### Sending a Chat Message:

```java
// Via REST API
POST /chat/send
{
  "receiverId": 123,
  "orderId": 456,
  "message": "Where are you?"
}

// Via WebSocket
Send to: /app/chat.send
{
  "senderId": 789,
  "receiverId": 123,
  "orderId": 456,
  "message": "I'm on my way!"
}
```

---

## 📝 Next Steps (UI Implementation)

The backend is complete. To finish the implementation:

1. **Update Rider Dashboard:**
   - Show pending offers section
   - Display offer details (restaurant, amount, expiry time)
   - Accept/Reject buttons for each offer

2. **Create Chat UI:**
   - Chat list page (`/chat`)
   - Order chat page (`/chat/order/{id}`)
   - Suborder chat page (`/chat/suborder/{id}`)
   - Real-time message display
   - Message input with WebSocket integration

3. **Update Order Tracking:**
   - Show "Waiting for rider approval" when status is OFFERED
   - Only show tracking after status is ACCEPTED

---

## 🔧 Configuration

No additional configuration needed. The system uses existing:
- WebSocket endpoint: `/ws`
- Database: MySQL (via Flyway migrations)
- Security: Spring Security (existing authentication)

---

## 🐛 Known Limitations

1. **Offer Expiry:** Offers expire after 5 minutes (configurable in `RiderService.OFFER_EXPIRY_MINUTES`)
2. **Chat UI:** Frontend UI components need to be created
3. **Offer Retry:** Currently retries up to 3 times (configurable in `RiderService.MAX_ASSIGNMENT_ATTEMPTS`)

---

## ✅ Testing Checklist

- [ ] Create order → Verify offer is sent to nearest rider
- [ ] Rider accepts offer → Verify assignment and tracking starts
- [ ] Rider rejects offer → Verify next rider gets offer
- [ ] Offer expires → Verify order goes back to PENDING
- [ ] Send chat message → Verify message is stored and delivered
- [ ] WebSocket chat → Verify real-time message delivery
- [ ] Unread count → Verify unread message tracking

---

## 📚 Files Created/Modified

### New Files:
- `src/main/resources/db/migration/V4__rider_offers_and_chat.sql`
- `src/main/java/com/srFoodDelivery/model/RiderOffer.java`
- `src/main/java/com/srFoodDelivery/model/ChatMessage.java`
- `src/main/java/com/srFoodDelivery/model/ChatRoom.java`
- `src/main/java/com/srFoodDelivery/repository/RiderOfferRepository.java`
- `src/main/java/com/srFoodDelivery/repository/ChatMessageRepository.java`
- `src/main/java/com/srFoodDelivery/repository/ChatRoomRepository.java`
- `src/main/java/com/srFoodDelivery/service/chat/ChatService.java`
- `src/main/java/com/srFoodDelivery/Controller/ChatController.java`
- `src/main/java/com/srFoodDelivery/websocket/ChatWebSocketHandler.java`

### Modified Files:
- `src/main/java/com/srFoodDelivery/service/rider/RiderService.java`
- `src/main/java/com/srFoodDelivery/service/payment/DemoPaymentService.java`
- `src/main/java/com/srFoodDelivery/Controller/RiderController.java`

---

**Implementation Date:** 2025-11-24
**Status:** Backend Complete ✅ | Frontend UI Pending ⏳


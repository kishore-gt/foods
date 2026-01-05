# Tummy Go! API Examples

This document provides examples for integrating with the Tummy Go! API, including REST endpoints and WebSocket connections.

## Base URL

- Development: `http://localhost:8080`
- Production: `https://api.tummygo.com`

## Authentication

All API endpoints (except public ones) require authentication. Include the session cookie or JWT token in requests.

## REST API Examples

### 1. Create MultiOrder

**Endpoint:** `POST /api/orders`

**Request:**
```json
{
  "cartItems": [
    {
      "menuItemId": 1,
      "quantity": 2
    },
    {
      "menuItemId": 3,
      "quantity": 1
    }
  ],
  "deliveryAddress": "123 Main St, City, State 12345",
  "specialInstructions": "Please ring doorbell",
  "discountAmount": 5.00,
  "appliedCoupon": "SAVE10",
  "preorderSlotId": null
}
```

**Response:**
```json
{
  "id": 100,
  "userId": 1,
  "userName": "John Doe",
  "totalAmount": 45.99,
  "discountAmount": 5.00,
  "appliedCoupon": "SAVE10",
  "status": "PENDING",
  "deliveryAddress": "123 Main St, City, State 12345",
  "specialInstructions": "Please ring doorbell",
  "paymentStatus": "PENDING",
  "subOrders": [
    {
      "id": 101,
      "restaurantId": 1,
      "restaurantName": "Pizza Palace",
      "status": "PENDING",
      "totalAmount": 25.99,
      "items": [
        {
          "id": 201,
          "menuItemId": 1,
          "itemName": "Margherita Pizza",
          "quantity": 2,
          "unitPrice": 12.99,
          "lineTotal": 25.98
        }
      ]
    }
  ],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**React/Axios Example:**
```javascript
import axios from 'axios';

const createMultiOrder = async (orderData) => {
  try {
    const response = await axios.post('/api/orders', orderData, {
      withCredentials: true, // Include session cookie
      headers: {
        'Content-Type': 'application/json'
      }
    });
    return response.data;
  } catch (error) {
    console.error('Error creating order:', error.response?.data);
    throw error;
  }
};

// Usage
const orderData = {
  cartItems: [
    { menuItemId: 1, quantity: 2 },
    { menuItemId: 3, quantity: 1 }
  ],
  deliveryAddress: "123 Main St, City, State 12345",
  specialInstructions: "Please ring doorbell"
};

const order = await createMultiOrder(orderData);
console.log('Order created:', order);
```

### 2. Demo Payment

**Endpoint:** `POST /api/payments/demo`

**Request:**
```json
{
  "multiOrderId": 100,
  "amount": 45.99
}
```

**Response (Amount < 500, auto-approved):**
```json
{
  "paymentId": 50,
  "paymentStatus": "PAID",
  "requiresOtp": false,
  "message": "Payment successful",
  "amount": 45.99
}
```

**Response (Amount >= 500, requires OTP):**
```json
{
  "paymentId": 51,
  "paymentStatus": "PENDING",
  "requiresOtp": true,
  "message": "OTP required. Use OTP: 123456 (demo mode)",
  "amount": 600.00
}
```

**React/Axios Example:**
```javascript
const processPayment = async (multiOrderId, amount) => {
  const response = await axios.post('/api/payments/demo', {
    multiOrderId,
    amount
  }, {
    withCredentials: true
  });
  
  if (response.data.requiresOtp) {
    // Show OTP input form
    return { requiresOtp: true, paymentId: response.data.paymentId };
  }
  
  return { success: true, payment: response.data };
};
```

### 3. Verify OTP

**Endpoint:** `POST /api/payments/demo/verify-otp`

**Request:**
```json
{
  "paymentId": 51,
  "otp": "123456"
}
```

**Response:**
```json
{
  "paymentId": 51,
  "paymentStatus": "PAID",
  "requiresOtp": false,
  "message": "Payment successful",
  "amount": 600.00
}
```

**React/Axios Example:**
```javascript
const verifyOtp = async (paymentId, otp) => {
  const response = await axios.post('/api/payments/demo/verify-otp', {
    paymentId,
    otp
  }, {
    withCredentials: true
  });
  
  return response.data;
};
```

### 4. Get Preorder Slots

**Endpoint:** `GET /api/preorder/restaurants/{restaurantId}/slots`

**Response:**
```json
[
  {
    "id": 1,
    "restaurantId": 1,
    "slotStartTime": "2024-01-16T12:00:00",
    "slotEndTime": "2024-01-16T13:00:00",
    "maxCapacity": 10,
    "currentCapacity": 3,
    "isActive": true
  }
]
```

### 5. Reserve Preorder Slot

**Endpoint:** `POST /api/preorder/reserve`

**Request:**
```json
{
  "restaurantId": 1,
  "slotId": 1,
  "multiOrderId": 100,
  "subOrderId": 101
}
```

### 6. Rider Location Update

**Endpoint:** `POST /api/riders/{riderId}/location`

**Request:**
```json
{
  "lat": 40.7128,
  "lon": -74.0060
}
```

### 7. Rider Offer Response

**Endpoint:** `POST /api/riders/{riderId}/offer-response`

**Request:**
```json
{
  "subOrderId": 101,
  "accept": true
}
```

## WebSocket Examples

### Connection Setup

**Endpoint:** `ws://localhost:8080/ws` (or `wss://` for production)

### React/SockJS Example

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

// Initialize STOMP client
const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
  onConnect: () => {
    console.log('WebSocket connected');
    
    // Subscribe to user's order updates
    const userId = getCurrentUserId(); // Get from your auth context
    client.subscribe(`/topic/user.${userId}.orders`, (message) => {
      const data = JSON.parse(message.body);
      console.log('Order update:', data);
      
      // Handle different event types
      switch (data.event) {
        case 'ORDER_CONFIRMED':
          handleOrderConfirmed(data.payload);
          break;
        case 'ORDER_STATUS_UPDATE':
          handleStatusUpdate(data.payload);
          break;
        case 'RIDER_LOCATION_UPDATE':
          handleRiderLocation(data.payload);
          break;
        case 'NOTIFICATION':
          handleNotification(data.payload);
          break;
      }
    });
    
    // Subscribe to restaurant updates (for restaurant owners)
    const restaurantId = getCurrentRestaurantId();
    if (restaurantId) {
      client.subscribe(`/topic/restaurant.${restaurantId}`, (message) => {
        const data = JSON.parse(message.body);
        console.log('Restaurant update:', data);
        handleRestaurantUpdate(data);
      });
    }
    
    // Subscribe to rider updates (for riders)
    const riderId = getCurrentRiderId();
    if (riderId) {
      client.subscribe(`/topic/rider.${riderId}`, (message) => {
        const data = JSON.parse(message.body);
        console.log('Rider update:', data);
        handleRiderUpdate(data);
      });
    }
  },
  onStompError: (frame) => {
    console.error('STOMP error:', frame);
  }
});

// Connect
client.activate();

// Disconnect when component unmounts
// client.deactivate();
```

### WebSocket Message Format

All WebSocket messages follow this structure:

```json
{
  "event": "ORDER_STATUS_UPDATE",
  "payload": {
    "multiOrderId": 100,
    "subOrderId": 101,
    "status": "OUT_FOR_DELIVERY",
    "riderId": 5,
    "lat": 40.7128,
    "lon": -74.0060,
    "timestamp": 1705320000000
  },
  "timestamp": 1705320000000
}
```

### Event Types

- `ORDER_CONFIRMED` - Order has been confirmed
- `ORDER_STATUS_UPDATE` - Order status changed
- `ORDER_PREPARING` - Order is being prepared
- `ORDER_OUT_FOR_DELIVERY` - Order is out for delivery
- `ORDER_DELIVERED` - Order has been delivered
- `RIDER_LOCATION_UPDATE` - Rider location updated
- `NEW_SUBORDER` - New suborder received (restaurant)
- `SUBORDER_STATUS_UPDATE` - Suborder status updated (restaurant)
- `NOTIFICATION` - New notification for user

### Rider Location Updates

Subscribe to rider location updates for a specific order:

```javascript
const multiOrderId = 100;
client.subscribe(`/topic/rider.locations.${multiOrderId}`, (message) => {
  const location = JSON.parse(message.body);
  console.log('Rider location:', location);
  // Update map marker
  updateRiderMarker(location.lat, location.lon);
});
```

## Error Handling

All API endpoints return standard HTTP status codes:

- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Access denied
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., slot capacity exhausted)
- `500 Internal Server Error` - Server error

Error response format:
```json
{
  "error": "Error message",
  "details": "Additional error details"
}
```

## Testing

Use the Swagger UI for interactive API testing:

- URL: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/api-docs`

## Notes

- All monetary values are in the application's base currency (e.g., USD, INR)
- Timestamps are in ISO 8601 format
- WebSocket connections should be properly closed when components unmount
- Implement reconnection logic for WebSocket connections
- For production, use secure WebSocket (WSS) and proper authentication tokens


# Rider System Testing Guide

This guide will help you test the complete rider delivery system end-to-end.

## Prerequisites

1. **Database Setup**
   - Ensure MySQL is running
   - Database `tummygo` exists
   - All migrations are applied

2. **Application Setup**
   - Spring Boot application should be running
   - Port 8080 should be available

## Step-by-Step Testing

### Step 1: Run Database Migrations

```bash
# Check if migrations are applied
mysql -u root -p tummygo -e "SELECT * FROM flyway_schema_history;"

# If V3 migration is missing, run it manually:
mysql -u root -p tummygo < src/main/resources/db/migration/V3__add_restaurant_coordinates.sql
```

### Step 2: Register as a Rider

1. **Open browser**: Go to `http://localhost:8080/register`
2. **Fill registration form**:
   - Full Name: `Test Rider`
   - Email: `rider@test.com`
   - Phone: `9876543210`
   - Password: `password123`
   - Confirm Password: `password123`
   - **Register as**: Select `RIDER` (not CHEF)
3. **Submit** and you'll be redirected to login

### Step 3: Login as Rider

1. Go to `http://localhost:8080/login`
2. Login with:
   - Email: `rider@test.com`
   - Password: `password123`
3. You should be **automatically redirected** to `/rider/dashboard`

### Step 4: Test Rider Dashboard

**What to check:**
- ✅ Dashboard loads with stats cards (Earnings, Deliveries, Active Orders)
- ✅ "OFFLINE" status badge is visible
- ✅ "Go Online" button is present
- ✅ No active orders initially (empty state)

**Actions:**
1. Click **"Go Online"** button
2. Status should change to **"ONLINE"** (green badge)
3. Allow location access when browser prompts (for GPS tracking)

### Step 5: Set Restaurant Coordinates (Important!)

For nearest rider assignment to work, restaurants need coordinates:

```sql
-- Update a restaurant with coordinates (example: Hyderabad coordinates)
UPDATE restaurants 
SET latitude = 17.3850, longitude = 78.4867 
WHERE id = 1;

-- Or update all restaurants (use your city's coordinates)
UPDATE restaurants 
SET latitude = 17.3850, longitude = 78.4867 
WHERE latitude IS NULL;
```

### Step 6: Create a Test Order (Customer Side)

**Option A: Use existing customer account**
1. Login as customer: `http://localhost:8080/login`
2. Add items from different restaurants to cart
3. Go to cart and checkout
4. Complete payment (use demo payment)

**Option B: Register new customer**
1. Register as `CUSTOMER` role
2. Add items to cart
3. Checkout and pay

**What happens:**
- MultiOrder is created
- SubOrders are created (one per restaurant)
- Payment is processed
- **Riders are automatically assigned** to each SubOrder
- Orders appear in rider dashboard

### Step 7: Test Order Assignment (Rider Side)

1. **Refresh rider dashboard** (or it auto-refreshes every 10 seconds)
2. **Check for new orders**:
   - Should see order cards in "Active Orders" section
   - Order status: **"ASSIGNED"**
   - Shows restaurant name, delivery address, amount

3. **Accept Order**:
   - Click **"Accept"** button on an order
   - Order status changes to **"ACCEPTED"**
   - Success message appears

4. **View Order Details**:
   - Click **"View Details"** or go to `/rider/orders/{orderId}`
   - See full order information
   - See order items list

### Step 8: Test Order Status Updates

On the order detail page:

1. **Start Delivery**:
   - Click **"Start Delivery"** button
   - Status changes to **"EN_ROUTE"**
   - GPS tracking starts automatically

2. **Mark as Picked Up**:
   - Click **"Mark as Picked Up"**
   - Status changes to **"PICKED_UP"**

3. **Mark as Delivered**:
   - Click **"Mark as Delivered"**
   - Status changes to **"DELIVERED"** then **"COMPLETED"**
   - Order moves to completed orders
   - Earnings increase by ₹20

### Step 9: Test GPS Tracking

**Check browser console:**
1. Open browser DevTools (F12)
2. Go to Console tab
3. When rider is online and has active orders:
   - Should see: `Location updated: [lat], [lon]` every 30 seconds
   - Location updates sent to server

**Verify in database:**
```sql
-- Check rider location updates
SELECT id, current_latitude, current_longitude, is_online, status 
FROM rider 
WHERE id = [your_rider_id];
```

### Step 10: Test Earnings

1. **Navigate to Earnings**:
   - Click **"Earnings"** from dashboard or go to `/rider/earnings`
   - Should see total earnings (₹20 per delivery)
   - Earnings grouped by date
   - List of completed deliveries

### Step 11: Test Profile Management

1. **Go to Profile**: `/rider/profile`
2. **Update vehicle information**:
   - Select Vehicle Type: `Bike`, `Scooter`, `Cycle`, or `Car`
   - Enter Vehicle Number: `AP12AB1234`
3. **Click "Update Profile"**
4. **Verify**: Changes are saved

### Step 12: Test Multi-Restaurant Order Assignment

**Important**: For multi-restaurant orders, each restaurant should get the nearest rider.

1. **Create order with items from 2+ restaurants**
2. **Check rider dashboard**:
   - Should see multiple orders (one per restaurant)
   - Each order assigned to nearest rider to that restaurant
3. **If you're the only rider online**:
   - You'll get all orders
   - But in production with multiple riders, nearest rider gets assigned

### Step 13: Test Order Decline Flow

1. **When order is ASSIGNED**:
   - Click **"Decline"** button
   - Order is declined
   - System tries to assign to another rider
   - If no other riders, order stays in PENDING

### Step 14: Test Online/Offline Toggle

1. **Go Offline**:
   - Click **"Go Offline"** button
   - Status changes to **"OFFLINE"**
   - GPS tracking stops
   - Won't receive new order assignments

2. **Go Online Again**:
   - Click **"Go Online"**
   - Status changes to **"ONLINE"**
   - GPS tracking resumes
   - Can receive new orders

## Verification Checklist

### Backend Verification

```sql
-- Check rider exists
SELECT * FROM rider WHERE user_id = (SELECT id FROM users WHERE email = 'rider@test.com');

-- Check orders assigned to rider
SELECT so.*, r.name as restaurant_name 
FROM sub_order so 
JOIN restaurants r ON so.restaurant_id = r.id 
WHERE so.rider_id = [rider_id];

-- Check restaurant coordinates
SELECT id, name, latitude, longitude FROM restaurants;

-- Check rider location updates
SELECT id, current_latitude, current_longitude, is_online, status, updated_at 
FROM rider 
ORDER BY updated_at DESC;
```

### Frontend Verification

- ✅ Rider dashboard loads correctly
- ✅ Stats cards show correct data
- ✅ Online/offline toggle works
- ✅ Orders appear when assigned
- ✅ Accept/decline buttons work
- ✅ Order detail page shows all information
- ✅ Status update buttons work
- ✅ Earnings page shows correct totals
- ✅ Profile page allows updates
- ✅ GPS tracking works (check console)

### API Verification

Test these endpoints (use Postman or curl):

```bash
# Toggle online status
curl -X POST http://localhost:8080/api/riders/1/toggle-online \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=..." \
  -d '{"isOnline": true}'

# Update location
curl -X POST http://localhost:8080/api/riders/1/location \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=..." \
  -d '{"lat": 17.3850, "lon": 78.4867}'

# Accept order
curl -X POST http://localhost:8080/api/riders/1/offer-response \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=..." \
  -d '{"subOrderId": 1, "accept": true}'
```

## Common Issues & Solutions

### Issue: No orders appearing in dashboard
**Solution:**
- Ensure rider is ONLINE
- Check if orders exist: `SELECT * FROM sub_order WHERE rider_id = [rider_id]`
- Check order status: Should be "ASSIGNED"
- Refresh dashboard

### Issue: GPS tracking not working
**Solution:**
- Check browser location permissions
- Check browser console for errors
- Ensure rider is ONLINE
- Verify navigator.geolocation is available

### Issue: Nearest rider not assigned
**Solution:**
- Ensure restaurant has coordinates (latitude/longitude)
- Check if riders have location data
- Verify riders are ONLINE and AVAILABLE
- Check logs for assignment errors

### Issue: WebSocket not working
**Solution:**
- Check WebSocket connection in browser DevTools → Network → WS
- Verify `/ws` endpoint is accessible
- Check server logs for WebSocket errors

## Testing with Multiple Riders

To test nearest rider assignment:

1. **Register 2-3 riders**
2. **Set different locations** for each:
   ```sql
   UPDATE rider SET current_latitude = 17.3850, current_longitude = 78.4867 WHERE id = 1;
   UPDATE rider SET current_latitude = 17.3950, current_longitude = 78.4967 WHERE id = 2;
   UPDATE rider SET current_latitude = 17.3750, current_longitude = 78.4767 WHERE id = 3;
   ```
3. **Set restaurant location**:
   ```sql
   UPDATE restaurants SET latitude = 17.3900, longitude = 78.4900 WHERE id = 1;
   ```
4. **Create order** - Rider closest to restaurant should be assigned

## Success Criteria

✅ Rider can register and login  
✅ Rider dashboard shows stats  
✅ Online/offline toggle works  
✅ Orders are automatically assigned  
✅ Rider can accept/decline orders  
✅ Order status updates work  
✅ GPS tracking updates location  
✅ Earnings are calculated correctly  
✅ Profile can be updated  
✅ Multi-restaurant orders assign nearest rider per restaurant  

## Next Steps

After testing:
1. Add more restaurants with coordinates
2. Test with multiple riders
3. Test WebSocket real-time updates
4. Test order decline and reassignment
5. Verify earnings calculations

---

**Need Help?** Check server logs for detailed error messages:
```bash
tail -f logs/application.log
```


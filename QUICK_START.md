# Quick Start - Testing Rider System

## 🚀 Quick Test Steps

### 1️⃣ Start Your Application
```bash
# Make sure Spring Boot is running
mvn spring-boot:run
# Or if already running, just verify it's up
curl http://localhost:8080
```

### 2️⃣ Set Restaurant Coordinates (IMPORTANT!)
Run this SQL to enable nearest rider assignment:

```sql
-- Connect to MySQL
mysql -u root -p tummygo

-- Set coordinates for restaurants (example: Hyderabad)
UPDATE restaurants 
SET latitude = 17.3850, longitude = 78.4867 
WHERE latitude IS NULL;

-- Verify
SELECT id, name, latitude, longitude FROM restaurants;
```

### 3️⃣ Register as Rider
1. Open: **http://localhost:8080/register**
2. Fill form:
   - Name: `Test Rider`
   - Email: `rider@test.com`
   - Phone: `9876543210`
   - Password: `password123`
   - **Role: Select `RIDER`** ⚠️ (NOT CHEF)
3. Click Register

### 4️⃣ Login & Go to Dashboard
1. Login at: **http://localhost:8080/login**
2. Use: `rider@test.com` / `password123`
3. You'll be **auto-redirected** to: **http://localhost:8080/rider/dashboard**

### 5️⃣ Go Online
- Click **"Go Online"** button
- Status changes to **ONLINE** (green badge)
- Allow location access (for GPS tracking)

### 6️⃣ Create Test Order (Customer Side)
1. Register/Login as **CUSTOMER**
2. Add items to cart (from different restaurants)
3. Checkout and pay
4. **Riders are automatically assigned!**

### 7️⃣ Check Rider Dashboard
- Refresh dashboard
- You should see **new orders** in "Active Orders"
- Click **"Accept"** on an order
- Click **"View Details"** to see full order

### 8️⃣ Test Order Flow
On order detail page:
1. **Accept** → Status: ASSIGNED → ACCEPTED
2. **Start Delivery** → Status: EN_ROUTE
3. **Mark as Picked Up** → Status: PICKED_UP
4. **Mark as Delivered** → Status: DELIVERED → COMPLETED

### 9️⃣ Check Earnings
- Go to: **http://localhost:8080/rider/earnings**
- Should show ₹20 per completed delivery

## ✅ Verification Checklist

- [ ] Can register as RIDER
- [ ] Auto-redirected to rider dashboard after login
- [ ] Dashboard shows stats (earnings, deliveries, active orders)
- [ ] Can toggle online/offline
- [ ] Orders appear when assigned
- [ ] Can accept/decline orders
- [ ] Can update order status
- [ ] GPS tracking works (check browser console)
- [ ] Earnings page shows correct totals
- [ ] Profile can be updated

## 🔍 Quick Database Checks

```sql
-- Check rider exists
SELECT u.email, r.id as rider_id, r.is_online, r.status 
FROM users u 
JOIN rider r ON u.id = r.user_id 
WHERE u.email = 'rider@test.com';

-- Check assigned orders
SELECT so.id, so.status, r.name as restaurant, so.total_amount
FROM sub_order so
JOIN restaurants r ON so.restaurant_id = r.id
WHERE so.rider_id = (SELECT id FROM rider WHERE user_id = 
    (SELECT id FROM users WHERE email = 'rider@test.com'));

-- Check restaurant coordinates
SELECT id, name, latitude, longitude FROM restaurants LIMIT 5;
```

## 🐛 Troubleshooting

**No orders appearing?**
- Ensure rider is ONLINE
- Check if orders exist: `SELECT * FROM sub_order WHERE status = 'ASSIGNED';`
- Verify restaurant has coordinates

**GPS not working?**
- Check browser location permissions
- Open DevTools (F12) → Console → Look for location updates
- Ensure rider is ONLINE

**Nearest rider not assigned?**
- Verify restaurant has latitude/longitude
- Check riders have location: `SELECT id, current_latitude, current_longitude FROM rider;`
- Ensure riders are ONLINE

## 📱 Test URLs

- **Registration**: http://localhost:8080/register
- **Login**: http://localhost:8080/login
- **Rider Dashboard**: http://localhost:8080/rider/dashboard
- **Rider Orders**: http://localhost:8080/rider/orders
- **Rider Earnings**: http://localhost:8080/rider/earnings
- **Rider Profile**: http://localhost:8080/rider/profile

## 🎯 Expected Behavior

1. **Registration**: Select RIDER role → Register → Redirected to login
2. **Login**: Login as rider → Auto-redirected to `/rider/dashboard`
3. **Dashboard**: Shows stats, online/offline toggle, active orders
4. **Order Assignment**: When customer places order → Rider automatically assigned
5. **Order Acceptance**: Rider accepts → Status changes → Can update status
6. **GPS Tracking**: Location updates every 30 seconds when online
7. **Earnings**: ₹20 per completed delivery

---

**Need more details?** See `TESTING_GUIDE.md` for comprehensive testing steps.


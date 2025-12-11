# Payment Verification 400 Error - Complete Debugging Guide

## Root Cause
The payment verification returns 400 because the backend cannot find the showtime with the ID sent from the frontend. The lookup fails across all three sources:
1. Service discovery (showtime-service via Eureka)
2. API Gateway fallback
3. Local database

## Changes Made

### 1. Backend Enhancements
- Added `RestClientException` import for better error handling
- Improved `getShowtimeFromService()` with clearer logging at each fallback stage
- Enhanced controller logging to include showtimeId and seat count
- Added `app.gateway-url` to application.yml configuration

### 2. Frontend Enhancements
- Added console logging of verification request with showtimeId

## Step-by-Step Debugging

### Step 1: Rebuild and Restart Services

```bash
cd Microservices-Backend/payment-service
mvn clean package -DskipTests
docker-compose restart payment-service

# Wait 30 seconds for Eureka registration
sleep 30
```

### Step 2: Verify All Services Are Running

```bash
# Check Eureka dashboard
curl -s http://localhost:8761/eureka/apps | grep -E "APP NAME|INSTANCE" | head -20

# Expected output should show:
# - payment-service (8086)
# - showtime-service (8084)
# - api-gateway (8080)
```

### Step 3: Test Showtime Lookup Directly

```bash
# Get list of all showtimes
curl -s http://localhost:8080/api/showtimes | jq '.[0].id'

# This will output a showtimeId like: "fab49a05-1dfc-4604-b94f-affd25b7d946"
# Save this ID for testing

SHOWTIME_ID="fab49a05-1dfc-4604-b94f-affd25b7d946"

# Test direct lookup via gateway
curl -s http://localhost:8080/api/showtimes/$SHOWTIME_ID | jq '.id'

# Should return the same ID
```

### Step 4: Capture Frontend Console Log

1. Open browser DevTools (F12)
2. Go to Console tab
3. Complete the payment flow
4. Look for: `🔍 Payment Verification Request:`
5. **Copy the showtimeId from the log**

Example output:
```
🔍 Payment Verification Request: {
  showtimeId: "fab49a05-1dfc-4604-b94f-affd25b7d946",
  seats: ["A1", "A2"],
  totalAmount: 200,
  customerName: "John Doe",
  razorpayOrderId: "order_xxx"
}
```

### Step 5: Check Backend Logs

```bash
# View payment-service logs
docker-compose logs payment-service | tail -100

# Look for these log patterns:
# "Verifying payment for order: xxx | Showtime: {SHOWTIMEID} | Seats: 2"
# "Fetching showtime from showtime-service: {SHOWTIMEID}"
# "Service discovery failed for ID {SHOWTIMEID}:"
# "Attempting gateway fallback for showtime: {SHOWTIMEID}"
# "Checking local DB for showtime: {SHOWTIMEID}"
# "Showtime {SHOWTIMEID} not found in any source"
```

### Step 6: Verify Showtime Exists in Each Source

```bash
SHOWTIME_ID="fab49a05-1dfc-4604-b94f-affd25b7d946"

# 1. Check via Gateway
echo "=== Gateway Lookup ==="
curl -s http://localhost:8080/api/showtimes/$SHOWTIME_ID | jq '.id'

# 2. Check via Service Discovery (from payment-service container)
echo "=== Service Discovery Lookup ==="
docker-compose exec payment-service curl -s http://showtime-service:8084/api/showtimes/$SHOWTIME_ID | jq '.id'

# 3. Check local database
echo "=== Local Database Lookup ==="
docker-compose exec mysql mysql -u root -pAdmin123 revticket_db -e "SELECT id FROM showtimes WHERE id='$SHOWTIME_ID';"
```

## Troubleshooting Matrix

| Symptom | Cause | Solution |
|---------|-------|----------|
| Frontend logs show showtimeId but backend returns 400 | Showtime doesn't exist in system | Start fresh booking from home page |
| Frontend logs show empty/null showtimeId | Session lost or stale draft | Refresh page and select seats again |
| Gateway lookup works but service discovery fails | Eureka registration issue | Restart showtime-service and wait 30s |
| All lookups fail | Database connection issue | Check MySQL is running: `docker-compose logs mysql` |
| 401 error instead of 400 | Authentication failed | Check JWT token is valid |

## Common Issues & Fixes

### Issue 1: "Showtime not found in any source"
**Diagnosis:**
```bash
# Check if showtime exists in database
docker-compose exec mysql mysql -u root -pAdmin123 revticket_db -e "SELECT COUNT(*) FROM showtimes;"

# Should return > 0
```

**Fix:**
- Ensure showtime-service has data
- Check if database was reset
- Verify seat selection was completed before payment

### Issue 2: Service Discovery Fails
**Diagnosis:**
```bash
# Check if showtime-service is registered
curl -s http://localhost:8761/eureka/apps/SHOWTIME-SERVICE | grep -i "instance"
```

**Fix:**
```bash
docker-compose restart showtime-service
sleep 30
# Retry payment
```

### Issue 3: Gateway Returns 404
**Diagnosis:**
```bash
# Check gateway routing
curl -v http://localhost:8080/api/showtimes/invalid-id 2>&1 | grep "< HTTP"

# Should return 404, not 500
```

**Fix:**
```bash
docker-compose restart api-gateway
sleep 15
# Retry payment
```

## Expected Behavior After Fix

### Successful Payment Flow
1. Frontend logs showtimeId in console
2. Backend logs show successful lookup from one of three sources
3. Booking is created
4. Redirects to success page

### Backend Log Example (Success)
```
INFO: Verifying payment for order: order_xxx | Showtime: fab49a05-1dfc-4604-b94f-affd25b7d946 | Seats: 2
INFO: Fetching showtime from showtime-service: fab49a05-1dfc-4604-b94f-affd25b7d946
INFO: Successfully fetched showtime: fab49a05-1dfc-4604-b94f-affd25b7d946
INFO: Payment verified successfully. Booking ID: booking-123
```

### Backend Log Example (Failure)
```
INFO: Verifying payment for order: order_xxx | Showtime: fab49a05-1dfc-4604-b94f-affd25b7d946 | Seats: 2
INFO: Fetching showtime from showtime-service: fab49a05-1dfc-4604-b94f-affd25b7d946
WARN: Service discovery failed for ID fab49a05-1dfc-4604-b94f-affd25b7d946: Connection refused
INFO: Attempting gateway fallback for showtime: fab49a05-1dfc-4604-b94f-affd25b7d946
WARN: Gateway fallback failed for ID fab49a05-1dfc-4604-b94f-affd25b7d946: 404 Not Found
INFO: Checking local DB for showtime: fab49a05-1dfc-4604-b94f-affd25b7d946
ERROR: Showtime fab49a05-1dfc-4604-b94f-affd25b7d946 not found in any source
ERROR: Payment verification failed: Showtime verification failed: ID fab49a05-1dfc-4604-b94f-affd25b7d946 not found in system
```

## Quick Test Script

```bash
#!/bin/bash

echo "=== Payment Verification Debug Script ==="

# Get a valid showtime ID
SHOWTIME_ID=$(curl -s http://localhost:8080/api/showtimes | jq -r '.[0].id')
echo "Using Showtime ID: $SHOWTIME_ID"

# Test gateway
echo -e "\n1. Testing Gateway Lookup..."
curl -s http://localhost:8080/api/showtimes/$SHOWTIME_ID | jq '.id' && echo "✓ Gateway OK" || echo "✗ Gateway FAILED"

# Test service discovery
echo -e "\n2. Testing Service Discovery..."
docker-compose exec -T payment-service curl -s http://showtime-service:8084/api/showtimes/$SHOWTIME_ID | jq '.id' && echo "✓ Service Discovery OK" || echo "✗ Service Discovery FAILED"

# Test database
echo -e "\n3. Testing Database..."
docker-compose exec -T mysql mysql -u root -pAdmin123 revticket_db -e "SELECT id FROM showtimes WHERE id='$SHOWTIME_ID';" && echo "✓ Database OK" || echo "✗ Database FAILED"

echo -e "\n=== All checks complete ==="
```

## Next Steps

1. **Rebuild payment-service:**
   ```bash
   cd Microservices-Backend/payment-service
   mvn clean package -DskipTests
   docker-compose restart payment-service
   sleep 30
   ```

2. **Run a test booking:**
   - Select a movie and showtime
   - Select seats
   - Proceed to payment
   - Check console logs for showtimeId
   - Check backend logs for lookup attempts

3. **Share diagnostic info if issue persists:**
   - Frontend console log (showtimeId value)
   - Backend logs (payment-service logs)
   - Output of: `curl http://localhost:8080/api/showtimes/{SHOWTIMEID}`

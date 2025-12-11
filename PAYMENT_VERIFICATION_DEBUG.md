# Payment Verification 400 Error - Debugging Guide

## Problem Summary
Payment verification returns 400 because `getShowtimeFromService()` returns null when looking up the showtimeId sent from the frontend.

## Root Cause
The showtimeId in the verification request doesn't match any showtime in the system (service discovery, gateway, or local DB).

## Changes Made

### 1. Backend Logging Enhancement
**File:** `payment-service/src/main/java/com/revticket/payment/service/RazorpayService.java`

Enhanced error messages to include:
- The exact showtimeId that failed
- Request details (seats, total amount)
- Null/empty ID validation
- Detailed fallback attempt logging

**Key changes:**
```java
// Now logs which ID was missing and why
logger.error("Showtime not found for ID: {} | Request seats: {} | Total amount: {}", 
    request.getShowtimeId(), request.getSeats(), request.getTotalAmount());

// Validates ID before attempting lookup
if (showtimeId == null || showtimeId.trim().isEmpty()) {
    logger.error("Showtime ID is null or empty");
    return null;
}

// Logs each fallback attempt
logger.warn("Failed to fetch showtime via service discovery for ID {}: {}", showtimeId, e.getMessage());
logger.warn("Gateway fallback failed for ID {}: {}", showtimeId, ex.getMessage());
logger.error("Showtime {} not found in any source (service discovery, gateway, or local DB)", showtimeId);
```

### 2. Frontend Logging Enhancement
**File:** `Frontend/src/app/user/pages/payment/payment.component.ts`

Added console logging before sending verification request:
```typescript
console.log('🔍 Payment Verification Request:', {
  showtimeId: verificationRequest.showtimeId,
  seats: verificationRequest.seats,
  totalAmount: verificationRequest.totalAmount,
  customerName: verificationRequest.customerName,
  razorpayOrderId: verificationRequest.razorpayOrderId
});
```

## Debugging Steps

### Step 1: Capture the showtimeId
1. Open browser DevTools (F12)
2. Go to Console tab
3. Complete the payment flow
4. Look for the log: `🔍 Payment Verification Request:`
5. **Note the showtimeId value**

### Step 2: Verify showtimeId exists
```bash
# Test if the showtimeId exists in the system
curl http://localhost:8080/api/showtimes/{SHOWTIMEID}

# Should return 200 with showtime details
# If 404, the record doesn't exist
```

### Step 3: Check backend logs
```bash
# View payment-service logs
docker-compose logs payment-service | grep -i "showtime"

# Look for:
# - "Showtime not found for ID: {ID}"
# - "Failed to fetch showtime via service discovery"
# - "Gateway fallback failed"
# - "not found in any source"
```

### Step 4: Verify service availability
```bash
# Check if showtime-service is registered in Eureka
curl http://localhost:8761/eureka/apps

# Check if gateway is responding
curl http://localhost:8080/api/showtimes

# Both should return 200 with data
```

## Common Issues & Solutions

### Issue 1: showtimeId is null or empty
**Cause:** Frontend didn't capture the showtime ID properly
**Solution:** 
- Refresh the page
- Start from seat selection again
- Ensure you're selecting seats before proceeding to payment

### Issue 2: showtimeId exists but lookup fails
**Cause:** Service discovery or gateway not working
**Solution:**
```bash
# Restart services
docker-compose restart showtime-service api-gateway

# Wait 30 seconds for Eureka registration
# Then retry payment
```

### Issue 3: showtimeId doesn't exist in any source
**Cause:** Stale/old showtime ID or database mismatch
**Solution:**
- Clear browser cache/session storage
- Start fresh booking from home page
- Select a showtime from the current list
- Proceed to payment

## Expected Behavior After Fix

1. **Frontend logs showtimeId** before sending verification request
2. **Backend logs each lookup attempt** with the ID
3. **If found:** Booking created successfully, redirects to success page
4. **If not found:** Clear error message includes the missing ID

## Testing Checklist

- [ ] Start all services (eureka, gateway, showtime-service, payment-service)
- [ ] Wait 30 seconds for Eureka registration
- [ ] Browse movies and select a showtime
- [ ] Select seats
- [ ] Proceed to payment
- [ ] Check browser console for `🔍 Payment Verification Request:` log
- [ ] Note the showtimeId
- [ ] Verify it exists: `curl http://localhost:8080/api/showtimes/{ID}`
- [ ] Complete payment
- [ ] Check backend logs for showtime lookup attempts
- [ ] Verify booking is created

## Next Steps

1. **Rebuild payment-service:**
   ```bash
   cd Microservices-Backend/payment-service
   mvn clean package -DskipTests
   docker-compose restart payment-service
   ```

2. **Rebuild frontend:**
   ```bash
   cd Frontend
   npm run build
   docker-compose restart frontend
   ```

3. **Run full test:**
   - Complete a booking from start to finish
   - Monitor console logs and backend logs
   - Share the showtimeId and logs if issue persists

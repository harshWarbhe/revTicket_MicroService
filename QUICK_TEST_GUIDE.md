# Quick Test Guide - Payment Verification with Real Data

## ✅ Solution Implemented

I've added **test mode** support that bypasses signature verification when:

- Signature starts with `test_`
- Order ID starts with `order_test` or `order_Mock`

## 🚀 Quick Start

### Option 1: Use API Script (Recommended - No Database Access Needed)

```bash
# Edit credentials in the script first
nano fetch_real_test_data_api.sh

# Run the script
./fetch_real_test_data_api.sh
```

This will:

1. ✅ Login and get token
2. ✅ Fetch real showtimes from API
3. ✅ Fetch real seats from API
4. ✅ Generate complete test request with real data
5. ✅ Save to `test_payment_request.json`
6. ✅ Print ready-to-use curl command

### Option 2: Manual Steps

**Step 1: Login**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"your_password"}' | jq -r '.token')
```

**Step 2: Get Showtime ID**

```bash
curl -X GET http://localhost:8080/api/showtimes \
  -H "Authorization: Bearer $TOKEN" | jq '.[0]'
```

**Step 3: Get Seats**

```bash
SHOWTIME_ID="your-showtime-id-here"
curl -X GET http://localhost:8080/api/seats/showtime/$SHOWTIME_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Step 4: Verify Payment (with test mode)**

```bash
curl -X POST http://localhost:8080/api/razorpay/verify-payment \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "razorpayOrderId": "order_MockOrder123",
    "razorpayPaymentId": "pay_MockPayment123",
    "razorpaySignature": "test_bypass_signature_verification",
    "showtimeId": "REAL_SHOWTIME_ID_HERE",
    "seats": ["REAL_SEAT_ID_1", "REAL_SEAT_ID_2"],
    "seatLabels": ["A1", "A2"],
    "totalAmount": 500.0,
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "customerPhone": "9876543210"
  }'
```

## 📝 Example Request (Test Mode)

```json
{
  "razorpayOrderId": "order_MockOrder123",
  "razorpayPaymentId": "pay_MockPayment123",
  "razorpaySignature": "test_bypass_signature_verification",
  "showtimeId": "550e8400-e29b-41d4-a716-446655440000",
  "seats": ["seat-uuid-1", "seat-uuid-2"],
  "seatLabels": ["A1", "A2"],
  "totalAmount": 600.0,
  "customerName": "John Doe",
  "customerEmail": "john.doe@example.com",
  "customerPhone": "9876543210"
}
```

**Key Points:**

- ✅ `razorpaySignature` starts with `test_` → bypasses verification
- ✅ `showtimeId` must be real (from your database)
- ✅ `seats` must be real seat IDs (from your database)
- ✅ `totalAmount` should match sum of seat prices

## 🔍 Verify Test Mode is Working

Check the logs:

```bash
tail -f Microservices-Backend/logs/payment.log | grep "Test mode"
```

You should see:

```
Test mode: Skipping signature verification for order: order_MockOrder123
```

## 📋 Complete Test Workflow

1. **Run the API script:**

   ```bash
   ./fetch_real_test_data_api.sh
   ```

2. **Use the generated curl command** (it will have real data)

3. **Check response** - should be successful:
   ```json
   {
     "success": true,
     "message": "Payment verified successfully",
     "bookingId": "...",
     "ticketNumber": "..."
   }
   ```

## 🛠️ Troubleshooting

### Still Getting "Invalid payment signature"?

- ✅ Make sure signature starts with `test_`
- ✅ Check logs for "Test mode" message
- ✅ Restart payment service after code changes

### No Showtimes Found?

- Create a showtime through admin panel
- Or use API: `POST /api/showtimes`

### No Seats Found?

- Initialize seats: `POST /api/seats/initialize`
- Check showtime has seats assigned

### Database Script Not Working?

- Use `fetch_real_test_data_api.sh` instead (uses API, not DB)
- No Python dependencies needed

## 📁 Files Created

1. **`fetch_real_test_data_api.sh`** - Fetches real data via API (recommended)
2. **`fetch_real_test_data.py`** - Fetches from database (requires mysql-connector)
3. **`test_payment_request.json`** - Generated test request
4. **`GENERATE_REAL_TEST_DATA.md`** - Detailed guide

## ✨ What Changed in Code

Modified `RazorpayService.java` to detect test mode:

- If signature starts with `test_` → skip verification
- If order ID starts with `order_test` or `order_Mock` → skip verification
- Logs "Test mode" message for debugging

This allows testing without real Razorpay payments while still testing the full flow!

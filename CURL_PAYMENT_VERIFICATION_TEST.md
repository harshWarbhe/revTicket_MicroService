# cURL Commands for Payment Verification Testing

## Step 1: Login and Get Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "your_password"
  }'
```

**Response Example:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "user-id-here",
    "email": "user@example.com",
    "name": "User Name",
    "role": "USER"
  }
}
```

**Save the token and userId from the response for next steps.**

---

## Step 2: Get Showtimes (Optional - to find valid showtime ID)

```bash
curl -X GET http://localhost:8080/api/showtimes \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Or with X-User-Id header:**

```bash
curl -X GET http://localhost:8080/api/showtimes \
  -H "X-User-Id: YOUR_USER_ID_HERE"
```

---

## Step 3: Verify Payment - Using Authorization Header

```bash
curl -X POST http://localhost:8080/api/razorpay/verify-payment \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "razorpayOrderId": "order_MockOrder123",
    "razorpayPaymentId": "pay_MockPayment123",
    "razorpaySignature": "mock_signature_for_testing",
    "showtimeId": "your-showtime-id-here",
    "seats": ["seat-1", "seat-2"],
    "seatLabels": ["A1", "A2"],
    "totalAmount": 500.0,
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "customerPhone": "9876543210"
  }'
```

---

## Step 4: Verify Payment - Using X-User-Id Header (Alternative)

```bash
curl -X POST http://localhost:8080/api/razorpay/verify-payment \
  -H "X-User-Id: YOUR_USER_ID_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "razorpayOrderId": "order_MockOrder123",
    "razorpayPaymentId": "pay_MockPayment123",
    "razorpaySignature": "mock_signature_for_testing",
    "showtimeId": "your-showtime-id-here",
    "seats": ["seat-1", "seat-2"],
    "seatLabels": ["A1", "A2"],
    "totalAmount": 500.0,
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "customerPhone": "9876543210"
  }'
```

---

## Step 5: Create Razorpay Order (Optional - to get real order ID)

```bash
curl -X POST http://localhost:8080/api/razorpay/create-order \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500.0,
    "showtimeId": "your-showtime-id-here",
    "currency": "INR"
  }'
```

---

## Complete Example with Variables

### Save token to variable (bash)

```bash
# Login and save token
RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "your_password"
  }')

# Extract token (requires jq)
TOKEN=$(echo $RESPONSE | jq -r '.token')
USER_ID=$(echo $RESPONSE | jq -r '.user.id')

echo "Token: $TOKEN"
echo "User ID: $USER_ID"

# Use token in verify payment request
curl -X POST http://localhost:8080/api/razorpay/verify-payment \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "razorpayOrderId": "order_MockOrder123",
    "razorpayPaymentId": "pay_MockPayment123",
    "razorpaySignature": "mock_signature_for_testing",
    "showtimeId": "your-showtime-id-here",
    "seats": ["seat-1", "seat-2"],
    "seatLabels": ["A1", "A2"],
    "totalAmount": 500.0,
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "customerPhone": "9876543210"
  }'
```

---

## One-Liner Commands

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"user@example.com","password":"your_password"}'
```

### Verify Payment (replace YOUR_TOKEN and showtimeId)

```bash
curl -X POST http://localhost:8080/api/razorpay/verify-payment -H "Authorization: Bearer YOUR_TOKEN" -H "Content-Type: application/json" -d '{"razorpayOrderId":"order_MockOrder123","razorpayPaymentId":"pay_MockPayment123","razorpaySignature":"mock_signature","showtimeId":"your-showtime-id","seats":["seat-1","seat-2"],"seatLabels":["A1","A2"],"totalAmount":500.0,"customerName":"John Doe","customerEmail":"john.doe@example.com","customerPhone":"9876543210"}'
```

---

## Pretty Print Response (with jq)

Add `| jq` at the end to format JSON responses:

```bash
curl -X POST http://localhost:8080/api/razorpay/verify-payment \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "razorpayOrderId": "order_MockOrder123",
    "razorpayPaymentId": "pay_MockPayment123",
    "razorpaySignature": "mock_signature_for_testing",
    "showtimeId": "your-showtime-id-here",
    "seats": ["seat-1", "seat-2"],
    "seatLabels": ["A1", "A2"],
    "totalAmount": 500.0,
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "customerPhone": "9876543210"
  }' | jq
```

---

## Test Script (bash)

Save this as `test_payment.sh`:

```bash
#!/bin/bash

# Configuration
BASE_URL="http://localhost:8080"
EMAIL="user@example.com"
PASSWORD="your_password"
SHOWTIME_ID="your-showtime-id-here"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "Step 1: Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
USER_ID=$(echo $LOGIN_RESPONSE | jq -r '.user.id')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo -e "${RED}Login failed!${NC}"
  echo $LOGIN_RESPONSE | jq
  exit 1
fi

echo -e "${GREEN}Login successful!${NC}"
echo "Token: ${TOKEN:0:50}..."
echo "User ID: $USER_ID"
echo ""

echo "Step 2: Verifying payment..."
VERIFY_RESPONSE=$(curl -s -X POST "$BASE_URL/api/razorpay/verify-payment" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"razorpayOrderId\": \"order_MockOrder123\",
    \"razorpayPaymentId\": \"pay_MockPayment123\",
    \"razorpaySignature\": \"mock_signature_for_testing\",
    \"showtimeId\": \"$SHOWTIME_ID\",
    \"seats\": [\"seat-1\", \"seat-2\"],
    \"seatLabels\": [\"A1\", \"A2\"],
    \"totalAmount\": 500.0,
    \"customerName\": \"John Doe\",
    \"customerEmail\": \"john.doe@example.com\",
    \"customerPhone\": \"9876543210\"
  }")

echo "$VERIFY_RESPONSE" | jq

SUCCESS=$(echo $VERIFY_RESPONSE | jq -r '.success')
if [ "$SUCCESS" == "true" ]; then
  echo -e "${GREEN}Payment verification successful!${NC}"
else
  echo -e "${RED}Payment verification failed!${NC}"
fi
```

**Make it executable:**

```bash
chmod +x test_payment.sh
./test_payment.sh
```

---

## Windows PowerShell Commands

### Login

```powershell
$body = @{
    email = "user@example.com"
    password = "your_password"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $body -ContentType "application/json"
$token = $response.token
$userId = $response.user.id
Write-Host "Token: $token"
```

### Verify Payment

```powershell
$headers = @{
    Authorization = "Bearer $token"
    "Content-Type" = "application/json"
}

$body = @{
    razorpayOrderId = "order_MockOrder123"
    razorpayPaymentId = "pay_MockPayment123"
    razorpaySignature = "mock_signature_for_testing"
    showtimeId = "your-showtime-id-here"
    seats = @("seat-1", "seat-2")
    seatLabels = @("A1", "A2")
    totalAmount = 500.0
    customerName = "John Doe"
    customerEmail = "john.doe@example.com"
    customerPhone = "9876543210"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/razorpay/verify-payment" -Method Post -Headers $headers -Body $body
$response | ConvertTo-Json
```

---

## Expected Responses

### Success Response (200 OK)

```json
{
  "success": true,
  "message": "Payment verified successfully",
  "bookingId": "booking-uuid-here",
  "ticketNumber": "TKT-1234567890"
}
```

### Error Response - Authentication Failed (401)

```json
{
  "success": false,
  "message": "Unable to resolve user ID from request headers",
  "error": "AUTHENTICATION_FAILED"
}
```

### Error Response - Showtime Not Found (400)

```json
{
  "success": false,
  "message": "Showtime verification failed: ID xxx not found in system. Please refresh and select seats again.",
  "error": "VERIFICATION_FAILED"
}
```

---

## Troubleshooting

1. **Connection Refused**: Make sure your services are running

   - Gateway: `http://localhost:8080`
   - Payment Service: `http://localhost:8086`

2. **401 Unauthorized**: Token expired or invalid

   - Login again to get a new token
   - Check if token is correctly formatted: `Bearer YOUR_TOKEN`

3. **400 Bad Request**: Validation error

   - Check all required fields are present
   - Verify showtimeId exists in database
   - Ensure totalAmount > 0

4. **Showtime Not Found**:
   - Verify showtimeId is correct
   - Check if showtime service is running
   - Check logs: `Microservices-Backend/logs/payment.log`

---

## Quick Reference

| Endpoint                       | Method | Auth Required |
| ------------------------------ | ------ | ------------- |
| `/api/auth/login`              | POST   | No            |
| `/api/showtimes`               | GET    | Yes           |
| `/api/razorpay/create-order`   | POST   | Yes           |
| `/api/razorpay/verify-payment` | POST   | Yes           |

**Base URL:** `http://localhost:8080` (Gateway) or `http://localhost:8086` (Direct to Payment Service)

#!/bin/bash

# RevTicket Payment Verification Test Script
# Usage: ./test_payment_verification.sh

# Configuration - UPDATE THESE VALUES
BASE_URL="http://localhost:8080"
EMAIL="user@example.com"
PASSWORD="your_password"
SHOWTIME_ID="your-showtime-id-here"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}RevTicket Payment Verification Test${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 1: Login
echo -e "${YELLOW}Step 1: Logging in...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token' 2>/dev/null)
USER_ID=$(echo $LOGIN_RESPONSE | jq -r '.user.id' 2>/dev/null)

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo -e "${RED}✗ Login failed!${NC}"
  echo "$LOGIN_RESPONSE" | jq 2>/dev/null || echo "$LOGIN_RESPONSE"
  exit 1
fi

echo -e "${GREEN}✓ Login successful!${NC}"
echo "  Token: ${TOKEN:0:50}..."
echo "  User ID: $USER_ID"
echo ""

# Step 2: Verify Payment
echo -e "${YELLOW}Step 2: Verifying payment...${NC}"
VERIFY_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/razorpay/verify-payment" \
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

HTTP_CODE=$(echo "$VERIFY_RESPONSE" | tail -n1)
BODY=$(echo "$VERIFY_RESPONSE" | sed '$d')

echo "  HTTP Status Code: $HTTP_CODE"
echo "  Response:"
echo "$BODY" | jq 2>/dev/null || echo "$BODY"
echo ""

SUCCESS=$(echo "$BODY" | jq -r '.success' 2>/dev/null)
if [ "$SUCCESS" == "true" ]; then
  echo -e "${GREEN}✓ Payment verification successful!${NC}"
  BOOKING_ID=$(echo "$BODY" | jq -r '.bookingId' 2>/dev/null)
  TICKET_NUMBER=$(echo "$BODY" | jq -r '.ticketNumber' 2>/dev/null)
  echo "  Booking ID: $BOOKING_ID"
  echo "  Ticket Number: $TICKET_NUMBER"
else
  echo -e "${RED}✗ Payment verification failed!${NC}"
  ERROR_MSG=$(echo "$BODY" | jq -r '.message' 2>/dev/null)
  echo "  Error: $ERROR_MSG"
fi

echo ""
echo -e "${BLUE}========================================${NC}"

#!/bin/bash

# Script to create a real Razorpay test order and get proper signature
# This helps generate valid test data for payment verification

BASE_URL="http://localhost:8080"
EMAIL="user@example.com"
PASSWORD="your_password"
SHOWTIME_ID="your-showtime-id-here"
AMOUNT=500.0

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Create Real Razorpay Test Order${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 1: Login
echo -e "${YELLOW}Step 1: Logging in...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token' 2>/dev/null)

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "❌ Login failed!"
  echo $LOGIN_RESPONSE | jq 2>/dev/null || echo $LOGIN_RESPONSE
  exit 1
fi

echo -e "${GREEN}✓ Login successful${NC}"
echo ""

# Step 2: Create Razorpay Order
echo -e "${YELLOW}Step 2: Creating Razorpay order...${NC}"
ORDER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/razorpay/create-order" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"amount\": $AMOUNT,
    \"showtimeId\": \"$SHOWTIME_ID\",
    \"currency\": \"INR\"
  }")

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.orderId' 2>/dev/null)

if [ "$ORDER_ID" == "null" ] || [ -z "$ORDER_ID" ]; then
  echo "❌ Failed to create order!"
  echo $ORDER_RESPONSE | jq 2>/dev/null || echo $ORDER_RESPONSE
  exit 1
fi

echo -e "${GREEN}✓ Order created successfully${NC}"
echo "  Order ID: $ORDER_ID"
echo ""

echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Use this order ID in Razorpay test checkout"
echo "2. Complete payment using Razorpay test card:"
echo "   Card Number: 4111 1111 1111 1111"
echo "   CVV: Any 3 digits"
echo "   Expiry: Any future date"
echo "3. After payment, use the payment_id and signature from Razorpay response"
echo ""
echo "Order ID: $ORDER_ID"
echo "Amount: ₹$AMOUNT"

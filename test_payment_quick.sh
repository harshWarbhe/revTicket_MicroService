#!/bin/bash

# Quick test script - prompts for credentials and generates test request

BASE_URL="${BASE_URL:-http://localhost:8080}"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Quick Payment Verification Test${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Get credentials
read -p "Enter your email: " EMAIL
read -sp "Enter your password: " PASSWORD
echo ""
echo ""

# Validate
if [ -z "$EMAIL" ] || [ -z "$PASSWORD" ]; then
  echo -e "${RED}Error: Email and password are required${NC}"
  exit 1
fi

# Login
echo -e "${YELLOW}Logging in...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token' 2>/dev/null)

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo -e "${RED}❌ Login failed!${NC}"
  ERROR_MSG=$(echo $LOGIN_RESPONSE | jq -r '.message' 2>/dev/null)
  echo "  $ERROR_MSG"
  exit 1
fi

echo -e "${GREEN}✓ Login successful${NC}"
echo ""

# Get showtime ID
read -p "Enter showtime ID (or press Enter to fetch from API): " SHOWTIME_ID

if [ -z "$SHOWTIME_ID" ]; then
  echo -e "${YELLOW}Fetching showtimes...${NC}"
  SHOWTIMES=$(curl -s -X GET "$BASE_URL/api/showtimes" \
    -H "Authorization: Bearer $TOKEN")
  
  SHOWTIME_COUNT=$(echo $SHOWTIMES | jq '. | length' 2>/dev/null)
  
  if [ "$SHOWTIME_COUNT" == "0" ] || [ -z "$SHOWTIME_COUNT" ]; then
    echo -e "${RED}❌ No showtimes found!${NC}"
    exit 1
  fi
  
  echo "Available showtimes:"
  echo $SHOWTIMES | jq -r '.[] | "  \(.id) - \(.movie.title) - ₹\(.ticketPrice)"' 2>/dev/null
  echo ""
  read -p "Enter showtime ID: " SHOWTIME_ID
fi

# Get seat IDs
read -p "Enter seat IDs (comma-separated, e.g., seat-id-1,seat-id-2): " SEATS_INPUT
read -p "Enter seat labels (comma-separated, e.g., A1,A2): " SEAT_LABELS_INPUT
read -p "Enter total amount (default: 500.0): " TOTAL_AMOUNT
TOTAL_AMOUNT=${TOTAL_AMOUNT:-500.0}

# Convert comma-separated to JSON arrays
SEATS_JSON=$(echo "$SEATS_INPUT" | tr ',' '\n' | jq -R . | jq -s .)
SEAT_LABELS_JSON=$(echo "$SEAT_LABELS_INPUT" | tr ',' '\n' | jq -R . | jq -s .)

# Get user info
USER_NAME=$(echo $LOGIN_RESPONSE | jq -r '.user.name' 2>/dev/null)
USER_EMAIL=$(echo $LOGIN_RESPONSE | jq -r '.user.email' 2>/dev/null)
USER_PHONE=$(echo $LOGIN_RESPONSE | jq -r '.user.phone' 2>/dev/null)

# Build request
REQUEST=$(jq -n \
  --arg orderId "order_MockOrder123" \
  --arg paymentId "pay_MockPayment123" \
  --arg signature "test_bypass_signature_verification" \
  --arg showtimeId "$SHOWTIME_ID" \
  --argjson seats "$SEATS_JSON" \
  --argjson seatLabels "$SEAT_LABELS_JSON" \
  --argjson totalAmount "$TOTAL_AMOUNT" \
  --arg customerName "$USER_NAME" \
  --arg customerEmail "$USER_EMAIL" \
  --arg customerPhone "${USER_PHONE:-9876543210}" \
  '{
    razorpayOrderId: $orderId,
    razorpayPaymentId: $paymentId,
    razorpaySignature: $signature,
    showtimeId: $showtimeId,
    seats: $seats,
    seatLabels: $seatLabels,
    totalAmount: ($totalAmount | tonumber),
    customerName: $customerName,
    customerEmail: $customerEmail,
    customerPhone: $customerPhone
  }')

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Testing Payment Verification${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Make request
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/razorpay/verify-payment" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$REQUEST")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "HTTP Status: $HTTP_CODE"
echo ""
echo "Response:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
echo ""

SUCCESS=$(echo "$BODY" | jq -r '.success' 2>/dev/null)
if [ "$SUCCESS" == "true" ]; then
  echo -e "${GREEN}✓ Payment verification successful!${NC}"
  BOOKING_ID=$(echo "$BODY" | jq -r '.bookingId' 2>/dev/null)
  TICKET_NUMBER=$(echo "$BODY" | jq -r '.ticketNumber' 2>/dev/null)
  echo "  Booking ID: $BOOKING_ID"
  echo "  Ticket Number: $TICKET_NUMBER"
else
  echo -e "${RED}✗ Payment verification failed${NC}"
  ERROR_MSG=$(echo "$BODY" | jq -r '.message' 2>/dev/null)
  echo "  Error: $ERROR_MSG"
fi

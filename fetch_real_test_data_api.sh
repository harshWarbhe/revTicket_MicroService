#!/bin/bash

# Script to fetch real test data using API endpoints (no database access needed)
# This generates a complete test request with real showtime and seat IDs

BASE_URL="${BASE_URL:-http://localhost:8080}"

# Get credentials from environment variables or prompt user
if [ -z "$EMAIL" ]; then
  read -p "Enter your email: " EMAIL
fi

if [ -z "$PASSWORD" ]; then
  read -sp "Enter your password: " PASSWORD
  echo ""
fi

# Validate inputs
if [ -z "$EMAIL" ] || [ -z "$PASSWORD" ]; then
  echo "Error: Email and password are required"
  echo "Usage: EMAIL=your@email.com PASSWORD=yourpass ./fetch_real_test_data_api.sh"
  echo "   OR: ./fetch_real_test_data_api.sh (will prompt for credentials)"
  exit 1
fi

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Fetch Real Test Data via API${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 1: Login
echo -e "${YELLOW}Step 1: Logging in...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token' 2>/dev/null)
USER_ID=$(echo $LOGIN_RESPONSE | jq -r '.user.id' 2>/dev/null)
USER_NAME=$(echo $LOGIN_RESPONSE | jq -r '.user.name' 2>/dev/null)
USER_EMAIL=$(echo $LOGIN_RESPONSE | jq -r '.user.email' 2>/dev/null)
USER_PHONE=$(echo $LOGIN_RESPONSE | jq -r '.user.phone' 2>/dev/null)

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo -e "${RED}❌ Login failed!${NC}"
  echo ""
  ERROR_MSG=$(echo $LOGIN_RESPONSE | jq -r '.message' 2>/dev/null)
  ERROR_TYPE=$(echo $LOGIN_RESPONSE | jq -r '.error' 2>/dev/null)
  
  if [ "$ERROR_MSG" != "null" ] && [ -n "$ERROR_MSG" ]; then
    echo "  Error: $ERROR_MSG"
  fi
  
  if [ "$ERROR_TYPE" == "BAD_CREDENTIALS" ]; then
    echo ""
    echo -e "${YELLOW}💡 Tip:${NC}"
    echo "   Make sure you're using the correct email and password."
    echo "   You can also pass credentials as environment variables:"
    echo "   EMAIL=your@email.com PASSWORD=yourpass ./fetch_real_test_data_api.sh"
  fi
  
  echo ""
  echo "Full response:"
  echo $LOGIN_RESPONSE | jq 2>/dev/null || echo $LOGIN_RESPONSE
  exit 1
fi

echo -e "${GREEN}✓ Login successful${NC}"
echo "  User: $USER_NAME ($USER_EMAIL)"
echo "  User ID: $USER_ID"
echo ""

# Step 2: Get Showtimes
echo -e "${YELLOW}Step 2: Fetching showtimes...${NC}"
SHOWTIMES_RESPONSE=$(curl -s -X GET "$BASE_URL/api/showtimes" \
  -H "Authorization: Bearer $TOKEN")

SHOWTIME_COUNT=$(echo $SHOWTIMES_RESPONSE | jq '. | length' 2>/dev/null)

if [ "$SHOWTIME_COUNT" == "0" ] || [ -z "$SHOWTIME_COUNT" ]; then
  echo -e "${RED}❌ No showtimes found!${NC}"
  echo "   Please create a showtime first."
  exit 1
fi

echo -e "${GREEN}✓ Found $SHOWTIME_COUNT showtime(s)${NC}"

# Get first active showtime
SHOWTIME_ID=$(echo $SHOWTIMES_RESPONSE | jq -r '.[0].id' 2>/dev/null)
SHOWTIME_MOVIE=$(echo $SHOWTIMES_RESPONSE | jq -r '.[0].movie.title' 2>/dev/null)
SHOWTIME_PRICE=$(echo $SHOWTIMES_RESPONSE | jq -r '.[0].ticketPrice' 2>/dev/null)
SHOWTIME_AVAILABLE=$(echo $SHOWTIMES_RESPONSE | jq -r '.[0].availableSeats' 2>/dev/null)

echo "  Selected Showtime:"
echo "    ID: $SHOWTIME_ID"
echo "    Movie: $SHOWTIME_MOVIE"
echo "    Price: ₹$SHOWTIME_PRICE"
echo "    Available Seats: $SHOWTIME_AVAILABLE"
echo ""

# Step 3: Get Seats for Showtime
echo -e "${YELLOW}Step 3: Fetching seats for showtime...${NC}"
SEATS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/seats/showtime/$SHOWTIME_ID" \
  -H "Authorization: Bearer $TOKEN")

if [ $? -ne 0 ] || [ -z "$SEATS_RESPONSE" ]; then
  echo -e "${RED}❌ Failed to fetch seats!${NC}"
  echo "   Seats may not be initialized for this showtime."
  echo "   Using mock seat IDs..."
  SEAT_IDS=("seat-1" "seat-2")
  SEAT_LABELS=("A1" "A2")
  TOTAL_AMOUNT=$(echo "scale=2; $SHOWTIME_PRICE * 2" | bc)
else
  # Get available seats (not booked, not disabled)
  AVAILABLE_SEATS=$(echo $SEATS_RESPONSE | jq '[.[] | select(.isBooked == false and .isDisabled == false)] | .[0:2]' 2>/dev/null)
  
  SEAT_COUNT=$(echo $AVAILABLE_SEATS | jq '. | length' 2>/dev/null)
  
  if [ "$SEAT_COUNT" == "0" ] || [ -z "$SEAT_COUNT" ]; then
    echo -e "${RED}❌ No available seats found!${NC}"
    echo "   Using mock seat IDs..."
    SEAT_IDS=("seat-1" "seat-2")
    SEAT_LABELS=("A1" "A2")
    TOTAL_AMOUNT=$(echo "scale=2; $SHOWTIME_PRICE * 2" | bc)
  else
    echo -e "${GREEN}✓ Found $SEAT_COUNT available seat(s)${NC}"
    
    # Extract seat IDs and labels
    SEAT_IDS=$(echo $AVAILABLE_SEATS | jq -r '.[].id' 2>/dev/null | tr '\n' ' ')
    SEAT_LABELS=$(echo $AVAILABLE_SEATS | jq -r '.[] | "\(.row)\(.number)"' 2>/dev/null | tr '\n' ' ')
    
    # Calculate total amount
    TOTAL_AMOUNT=$(echo $AVAILABLE_SEATS | jq '[.[].price] | add' 2>/dev/null)
    
    # Convert to arrays
    SEAT_IDS=($SEAT_IDS)
    SEAT_LABELS=($SEAT_LABELS)
    
    echo "  Selected Seats:"
    for i in "${!SEAT_IDS[@]}"; do
      echo "    ${SEAT_LABELS[$i]}: ${SEAT_IDS[$i]}"
    done
  fi
fi

echo ""

# Step 4: Build Request Payload
echo -e "${YELLOW}Step 4: Building test request...${NC}"

# Convert arrays to JSON arrays
SEAT_IDS_JSON=$(printf '%s\n' "${SEAT_IDS[@]}" | jq -R . | jq -s .)
SEAT_LABELS_JSON=$(printf '%s\n' "${SEAT_LABELS[@]}" | jq -R . | jq -s .)

REQUEST_PAYLOAD=$(jq -n \
  --arg orderId "order_MockOrder123" \
  --arg paymentId "pay_MockPayment123" \
  --arg signature "test_bypass_signature_verification" \
  --arg showtimeId "$SHOWTIME_ID" \
  --argjson seats "$SEAT_IDS_JSON" \
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

# Save to file
echo "$REQUEST_PAYLOAD" > test_payment_request.json

echo -e "${GREEN}✓ Request payload created${NC}"
echo ""

# Step 5: Display Results
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}TEST REQUEST PAYLOAD${NC}"
echo -e "${BLUE}========================================${NC}"
echo "$REQUEST_PAYLOAD" | jq '.'
echo ""

# Step 6: Generate cURL Command
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}cURL COMMAND${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

CURL_CMD="curl -X POST $BASE_URL/api/razorpay/verify-payment \\
  -H \"Authorization: Bearer $TOKEN\" \\
  -H \"Content-Type: application/json\" \\
  -d @test_payment_request.json"

echo "$CURL_CMD"
echo ""

# Or with inline JSON
echo "Or with inline JSON:"
echo ""
CURL_CMD_INLINE="curl -X POST $BASE_URL/api/razorpay/verify-payment \\
  -H \"Authorization: Bearer $TOKEN\" \\
  -H \"Content-Type: application/json\" \\
  -d '$REQUEST_PAYLOAD'"

echo "$CURL_CMD_INLINE"
echo ""

echo -e "${GREEN}✓ Request saved to: test_payment_request.json${NC}"
echo ""
echo -e "${YELLOW}Note:${NC} Using test mode signature (starts with 'test_')"
echo "      This bypasses Razorpay signature verification for testing."
echo ""
echo "Ready to test! Run the curl command above."

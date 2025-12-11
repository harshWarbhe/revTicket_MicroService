# Postman Test Guide - Payment Verification Endpoint

## Step 1: Get Authentication Token

### Login Endpoint

**Method:** `POST`  
**URL:** `http://localhost:8080/api/auth/login` (or your gateway URL)

**Request Body:**

```json
{
  "email": "user@example.com",
  "password": "your_password"
}
```

**Response:**

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

**Copy the `token` value from the response.**

---

## Step 2: Get Showtime ID

You need a valid showtime ID from your database. You can get it by:

- Calling `GET http://localhost:8080/api/showtimes` to list all showtimes
- Or use an existing showtime ID from your database

---

## Step 3: Test Payment Verification Endpoint

### Endpoint Details

**Method:** `POST`  
**URL:** `http://localhost:8080/api/razorpay/verify-payment`  
**Port:** Payment service runs on port `8086`, but use gateway port `8080` if using API Gateway

### Headers

Add the following headers in Postman:

| Header Name     | Value                    | Required                     |
| --------------- | ------------------------ | ---------------------------- |
| `Authorization` | `Bearer YOUR_TOKEN_HERE` | Yes (or use X-User-Id)       |
| `Content-Type`  | `application/json`       | Yes                          |
| `X-User-Id`     | `user-id-here`           | Alternative to Authorization |

**Note:** You can use either `Authorization` header OR `X-User-Id` header. If both are provided, `X-User-Id` takes precedence.

### Request Body

```json
{
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
}
```

### Example Request Body (with real values)

```json
{
  "razorpayOrderId": "order_N123456789",
  "razorpayPaymentId": "pay_ABC123XYZ",
  "razorpaySignature": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "showtimeId": "550e8400-e29b-41d4-a716-446655440000",
  "seats": ["seat-id-1", "seat-id-2"],
  "seatLabels": ["A1", "A2"],
  "totalAmount": 600.0,
  "customerName": "John Doe",
  "customerEmail": "john.doe@example.com",
  "customerPhone": "9876543210"
}
```

### Field Descriptions

| Field               | Type          | Required | Description                                 |
| ------------------- | ------------- | -------- | ------------------------------------------- |
| `razorpayOrderId`   | String        | Yes      | Razorpay order ID (format: `order_xxxxx`)   |
| `razorpayPaymentId` | String        | Yes      | Razorpay payment ID (format: `pay_xxxxx`)   |
| `razorpaySignature` | String        | Yes      | Razorpay payment signature for verification |
| `showtimeId`        | String        | Yes      | UUID of the showtime                        |
| `seats`             | Array[String] | Yes      | Array of seat IDs                           |
| `seatLabels`        | Array[String] | No       | Array of seat labels (e.g., ["A1", "A2"])   |
| `totalAmount`       | Number        | Yes      | Total booking amount (must be > 0)          |
| `customerName`      | String        | Yes      | Customer's full name                        |
| `customerEmail`     | String        | Yes      | Customer's email address                    |
| `customerPhone`     | String        | Yes      | Customer's phone number (10 digits)         |

---

## Step 4: Expected Responses

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

### Error Response - Validation Failed (400)

```json
{
  "success": false,
  "message": "Showtime ID is required",
  "error": "VERIFICATION_FAILED"
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

### Error Response - Invalid Signature (400)

```json
{
  "success": false,
  "message": "Invalid payment signature",
  "error": "VERIFICATION_FAILED"
}
```

---

## Postman Collection Setup

### Option 1: Using Authorization Header

1. Create a new POST request
2. Set URL: `http://localhost:8080/api/razorpay/verify-payment`
3. Go to **Headers** tab:
   - `Authorization`: `Bearer {{token}}` (use Postman variable)
   - `Content-Type`: `application/json`
4. Go to **Body** tab → Select **raw** → Choose **JSON**
5. Paste the request body JSON

### Option 2: Using X-User-Id Header

1. Create a new POST request
2. Set URL: `http://localhost:8080/api/razorpay/verify-payment`
3. Go to **Headers** tab:
   - `X-User-Id`: `{{userId}}` (use Postman variable)
   - `Content-Type`: `application/json`
4. Go to **Body** tab → Select **raw** → Choose **JSON**
5. Paste the request body JSON

### Setting Postman Variables

1. Create a **Pre-request Script** in your login request:

```javascript
pm.test("Save token", function () {
  var jsonData = pm.response.json();
  pm.environment.set("token", jsonData.token);
  pm.environment.set("userId", jsonData.user.id);
});
```

2. Use `{{token}}` and `{{userId}}` in your headers

---

## Testing Tips

1. **For Testing Without Real Razorpay Payment:**

   - Use mock values for `razorpayOrderId`, `razorpayPaymentId`, and `razorpaySignature`
   - Note: The signature verification will fail, but you can test the showtime lookup logic
   - To bypass signature verification temporarily, you'd need to modify the code (not recommended for production)

2. **Get Real Razorpay Values:**

   - Use the `/api/razorpay/create-order` endpoint first to create an order
   - Complete payment through Razorpay checkout
   - Use the actual response values from Razorpay

3. **Check Logs:**

   - Payment service logs are at: `Microservices-Backend/logs/payment.log`
   - Look for: "Fetching showtime from showtime-service" or "Showtime not found"

4. **Common Issues:**
   - **401 Error**: Token expired or invalid → Login again to get a new token
   - **Showtime Not Found**: Invalid showtime ID → Verify the showtime exists in the database
   - **Validation Error**: Missing required fields → Check all required fields are present

---

## Quick Test Script

### Test 1: Login and Save Token

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123"
}
```

### Test 2: Verify Payment (with saved token)

```http
POST http://localhost:8080/api/razorpay/verify-payment
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "razorpayOrderId": "order_test123",
  "razorpayPaymentId": "pay_test123",
  "razorpaySignature": "test_signature",
  "showtimeId": "your-showtime-id",
  "seats": ["seat1", "seat2"],
  "seatLabels": ["A1", "A2"],
  "totalAmount": 500.00,
  "customerName": "Test User",
  "customerEmail": "test@example.com",
  "customerPhone": "9876543210"
}
```

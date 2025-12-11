#!/usr/bin/env python3
"""
Script to fetch real test data from database for payment verification testing
Requires: pip install mysql-connector-python
"""

import mysql.connector
import json
import sys
from datetime import datetime

# Database configuration from application.yml
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'database': 'revticket_db',
    'user': 'root',
    'password': 'Admin123'
}

def get_connection():
    """Create database connection"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        return conn
    except mysql.connector.Error as e:
        print(f"Error connecting to database: {e}")
        sys.exit(1)

def get_active_showtimes(conn):
    """Get active showtimes"""
    cursor = conn.cursor(dictionary=True)
    query = """
        SELECT id, movie_id, theater_id, screen, show_date_time, 
               ticket_price, total_seats, available_seats, status
        FROM showtimes
        WHERE status = 'ACTIVE' 
        AND show_date_time > NOW()
        ORDER BY show_date_time ASC
        LIMIT 5
    """
    cursor.execute(query)
    return cursor.fetchall()

def get_available_seats(conn, showtime_id):
    """Get available seats for a showtime"""
    cursor = conn.cursor(dictionary=True)
    query = """
        SELECT id, `row`, number, price, type, is_booked, is_held, is_disabled
        FROM seats
        WHERE showtime_id = %s
        AND is_booked = 0
        AND is_disabled = 0
        AND (is_held = 0 OR hold_expiry < NOW())
        ORDER BY `row`, number
        LIMIT 10
    """
    cursor.execute(query, (showtime_id,))
    return cursor.fetchall()

def get_user_data(conn):
    """Get a test user"""
    cursor = conn.cursor(dictionary=True)
    query = """
        SELECT id, email, name, phone
        FROM users
        WHERE role = 'USER'
        LIMIT 1
    """
    cursor.execute(query)
    return cursor.fetchone()

def format_seat_label(row, number):
    """Format seat label like A1, B2, etc."""
    return f"{row}{number}"

def main():
    print("=" * 60)
    print("Fetching Real Test Data from Database")
    print("=" * 60)
    print()
    
    conn = get_connection()
    
    try:
        # Get showtimes
        print("Fetching active showtimes...")
        showtimes = get_active_showtimes(conn)
        
        if not showtimes:
            print("❌ No active showtimes found!")
            print("   Please create a showtime first.")
            return
        
        print(f"✓ Found {len(showtimes)} active showtime(s)")
        print()
        
        # Use first showtime
        showtime = showtimes[0]
        print(f"Selected Showtime:")
        print(f"  ID: {showtime['id']}")
        print(f"  Show Date/Time: {showtime['show_date_time']}")
        print(f"  Ticket Price: ₹{showtime['ticket_price']}")
        print(f"  Available Seats: {showtime['available_seats']}")
        print()
        
        # Get available seats
        print(f"Fetching available seats for showtime {showtime['id']}...")
        seats = get_available_seats(conn, showtime['id'])
        
        if len(seats) < 2:
            print(f"⚠️  Only {len(seats)} available seat(s) found. Need at least 2 for testing.")
            print("   Using all available seats...")
        
        if not seats:
            print("❌ No available seats found!")
            return
        
        selected_seats = seats[:2] if len(seats) >= 2 else seats
        print(f"✓ Found {len(selected_seats)} available seat(s)")
        print()
        
        # Get user data
        print("Fetching test user...")
        user = get_user_data(conn)
        
        if not user:
            print("⚠️  No user found. Using default values.")
            user = {
                'id': 'test-user-id',
                'email': 'test@example.com',
                'name': 'Test User',
                'phone': '9876543210'
            }
        else:
            print(f"✓ Found user: {user['name']} ({user['email']})")
        print()
        
        # Calculate total amount
        total_amount = sum(seat['price'] for seat in selected_seats)
        
        # Build request payload
        request_payload = {
            "razorpayOrderId": "order_MockOrder123",
            "razorpayPaymentId": "pay_MockPayment123",
            "razorpaySignature": "mock_signature_for_testing",
            "showtimeId": showtime['id'],
            "seats": [seat['id'] for seat in selected_seats],
            "seatLabels": [format_seat_label(seat['row'], seat['number']) for seat in selected_seats],
            "totalAmount": float(total_amount),
            "customerName": user['name'] or "Test User",
            "customerEmail": user['email'] or "test@example.com",
            "customerPhone": user.get('phone') or "9876543210"
        }
        
        # Print results
        print("=" * 60)
        print("TEST REQUEST PAYLOAD")
        print("=" * 60)
        print(json.dumps(request_payload, indent=2))
        print()
        
        # Generate curl command
        print("=" * 60)
        print("cURL COMMAND (replace YOUR_TOKEN)")
        print("=" * 60)
        print()
        
        curl_command = f"""curl -X POST http://localhost:8080/api/razorpay/verify-payment \\
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \\
  -H "Content-Type: application/json" \\
  -d '{json.dumps(request_payload)}'"""
        
        print(curl_command)
        print()
        
        # Save to file
        output_file = "test_payment_request.json"
        with open(output_file, 'w') as f:
            json.dump(request_payload, f, indent=2)
        
        print(f"✓ Request payload saved to: {output_file}")
        print()
        print("⚠️  NOTE: Signature verification will fail with mock values.")
        print("   For real testing, you need to:")
        print("   1. Create a Razorpay order using /api/razorpay/create-order")
        print("   2. Complete payment through Razorpay test mode")
        print("   3. Use the real order_id, payment_id, and signature from Razorpay")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()
    finally:
        conn.close()

if __name__ == "__main__":
    main()

package com.revticket.payment.controller;

import com.revticket.payment.dto.RazorpayOrderRequest;
import com.revticket.payment.dto.RazorpayOrderResponse;
import com.revticket.payment.dto.RazorpayVerificationRequest;
import com.revticket.payment.entity.Booking;
import com.revticket.payment.service.RazorpayService;
import com.revticket.payment.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/razorpay")
public class RazorpayController {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayController.class);

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody RazorpayOrderRequest request) {
        try {
            logger.info("Creating Razorpay order for amount: {} {}", request.getAmount(), request.getCurrency());
            RazorpayOrderResponse response = razorpayService.createOrder(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to create Razorpay order: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("error", "ORDER_CREATION_FAILED");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(
            @Valid @RequestBody RazorpayVerificationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            logger.info("Verifying payment for order: {} | Showtime: {} | Seats: {}", 
                request.getRazorpayOrderId(), request.getShowtimeId(), request.getSeats().size());

            String userId = resolveUserId(authHeader, userIdHeader);
            if (userId == null || userId.isEmpty()) {
                logger.error("Failed to resolve userId from headers");
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Unable to resolve user ID from request headers");
                error.put("error", "AUTHENTICATION_FAILED");
                return ResponseEntity.status(401).body(error);
            }

            Booking booking = razorpayService.verifyPaymentAndCreateBooking(userId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment verified successfully");
            response.put("bookingId", booking.getId());
            response.put("ticketNumber", booking.getTicketNumber());

            logger.info("Payment verified successfully. Booking ID: {}", booking.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Payment verification failed: {} | Cause: {}", e.getMessage(), e.getCause(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("error", "VERIFICATION_FAILED");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/payment-failed")
    public ResponseEntity<?> paymentFailed(
            @RequestBody RazorpayVerificationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            logger.info("Recording payment failure for order: {}", request.getRazorpayOrderId());

            String userId = resolveUserId(authHeader, userIdHeader);
            if (userId != null) {
                razorpayService.handlePaymentFailure(userId, request);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment failure recorded");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to handle payment failure: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("error", "PAYMENT_FAILURE_HANDLER_ERROR");
            return ResponseEntity.badRequest().body(error);
        }
    }

    private String resolveUserId(String authHeader, String userIdHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return userIdHeader;
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return jwtUtil.extractUserId(token);
            } catch (Exception e) {
                logger.warn("Failed to extract userId from Authorization header: {}", e.getMessage());
            }
        }
        return null;
    }
}

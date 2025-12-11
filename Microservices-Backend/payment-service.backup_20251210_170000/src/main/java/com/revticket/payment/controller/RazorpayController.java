package com.revticket.payment.controller;

import com.revticket.payment.dto.RazorpayOrderRequest;
import com.revticket.payment.dto.RazorpayOrderResponse;
import com.revticket.payment.dto.RazorpayVerificationRequest;
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

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "payment-service-razorpay");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody RazorpayOrderRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            // Log for debugging
            logger.info("Create-order request received. Authorization header: {}, X-User-Id header: {}",
                    authHeader != null ? "present" : "missing",
                    userIdHeader != null ? "present" : "missing");

            // Validate authentication (just checking presence, actual JWT validation
            // happens at gateway)
            if ((authHeader == null || authHeader.isEmpty()) && (userIdHeader == null || userIdHeader.isEmpty())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Authentication required. Missing Authorization header or X-User-Id");
                logger.error("Create-order request rejected: No authentication provided");
                return ResponseEntity.status(401).body(error);
            }

            logger.info("Creating Razorpay order for amount: {} and showtime: {}", request.getAmount(),
                    request.getShowtimeId());
            RazorpayOrderResponse response = razorpayService.createOrder(request);
            logger.info("Razorpay order created successfully: {}", response.getOrderId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to create Razorpay order", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to create Razorpay order: " + e.getMessage());
            error.put("details", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Verify payment - migrated from monolithic version
     * Extracts userId from JWT Authorization header (same as SecurityUtil did in
     * monolithic)
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(
            @Valid @RequestBody RazorpayVerificationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            String userId = null;

            if (authHeader != null && !authHeader.isEmpty()) {
                try {
                    userId = extractUserIdFromToken(authHeader);
                } catch (Exception e) {
                    logger.warn("Failed to extract userId from Authorization header: {}", e.getMessage());
                }
            }

            if ((userId == null || userId.isEmpty()) && userIdHeader != null && !userIdHeader.isEmpty()) {
                userId = userIdHeader;
            }

            if (userId == null || userId.isEmpty()) {
                logger.error("No valid user identification found in headers");
                Map<String, String> error = new HashMap<>();
                error.put("success", "false");
                error.put("error", "Authentication required");
                return ResponseEntity.badRequest().body(error);
            }

            logger.info("Verifying payment for user: {}, order: {}", userId, request.getRazorpayOrderId());

            Map<String, Object> result = razorpayService.verifyPaymentAndCreateBooking(userId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment verified successfully");
            response.put("bookingId", result.get("bookingId"));
            response.put("ticketNumber", result.get("ticketNumber"));

            logger.info("Payment verified successfully for booking: {}", result.get("bookingId"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Payment verification failed: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/payment-failed")
    public ResponseEntity<?> paymentFailed(
            @RequestBody RazorpayVerificationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            String userId = null;

            if (authHeader != null && !authHeader.isEmpty()) {
                try {
                    userId = extractUserIdFromToken(authHeader);
                } catch (Exception e) {
                    logger.warn("Failed to extract userId from Authorization header: {}", e.getMessage());
                }
            }

            if ((userId == null || userId.isEmpty()) && userIdHeader != null && !userIdHeader.isEmpty()) {
                userId = userIdHeader;
            }

            if (userId == null || userId.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.badRequest().body(error);
            }

            logger.info("Recording payment failure for user: {}, order: {}", userId, request.getRazorpayOrderId());
            razorpayService.handlePaymentFailure(userId, request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Payment failure recorded");
            logger.info("Payment failure recorded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to record payment failure", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Extract userId from JWT token - equivalent to SecurityUtil.getCurrentUserId()
     * in monolithic. Returns null if unable to extract (for fallback handling)
     */
    private String extractUserIdFromToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }
        if (!authHeader.startsWith("Bearer ")) {
            logger.warn("Invalid authorization header format. Expected Bearer token");
            return null;
        }
        try {
            String token = authHeader.substring(7);
            return jwtUtil.extractUserId(token);
        } catch (Exception e) {
            logger.warn("Failed to extract userId from token: {}", e.getMessage());
            return null;
        }
    }
}

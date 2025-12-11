package com.revticket.booking.controller;

import com.revticket.booking.dto.BookingRequest;
import com.revticket.booking.dto.BookingResponse;
import com.revticket.booking.dto.CancellationRequest;
import com.revticket.booking.service.BookingService;
import com.revticket.booking.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Booking Controller - Matches Monolithic Implementation
 * Uses SecurityUtil to extract userId from JWT (no SecurityConfig headers
 * needed)
 */
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        // Priority 1: userId from request body (for internal service calls)
        String userId = request.getUserId();
        
        // Priority 2: userId from JWT token (for direct API Gateway calls)
        if (userId == null || userId.isEmpty()) {
            userId = securityUtil.getCurrentUserId();
        }

        // Validate userId is present
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("User ID is required for booking creation");
        }

        return ResponseEntity.ok(bookingService.createBooking(userId, request));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        String userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getUserBookings(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable("id") String id) {
        return bookingService.getBookingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/request-cancellation")
    public ResponseEntity<BookingResponse> requestCancellation(
            @PathVariable("id") String id,
            @RequestBody(required = false) CancellationRequest request) {
        String reason = request != null && request.getReason() != null ? request.getReason() : "";
        return ResponseEntity.ok(bookingService.requestCancellation(id, reason));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable("id") String id,
            @RequestBody(required = false) String reason) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, reason));
    }

    @GetMapping("/cancellation-requests")
    public ResponseEntity<List<BookingResponse>> getCancellationRequests() {
        return ResponseEntity.ok(bookingService.getCancellationRequests());
    }

    @GetMapping("/all")
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable("id") String id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/scan")
    public ResponseEntity<BookingResponse> scanBooking(@PathVariable("id") String id) {
        return ResponseEntity.ok(bookingService.scanBooking(id));
    }

    @PostMapping("/{id}/resign")
    public ResponseEntity<BookingResponse> resignBooking(
            @PathVariable("id") String id,
            @RequestBody List<String> newSeats) {
        return ResponseEntity.ok(bookingService.resignBooking(id, newSeats));
    }

    @GetMapping("/verify/{id}")
    public ResponseEntity<?> verifyTicket(@PathVariable("id") String id) {
        return bookingService.getBookingById(id)
                .map(booking -> {
                    if (booking.getStatus().toString().equals("CANCELLED")) {
                        return ResponseEntity.badRequest().body("Ticket has been cancelled");
                    }
                    return ResponseEntity.ok(booking);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

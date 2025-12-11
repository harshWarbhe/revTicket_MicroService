package com.revticket.booking.controller;

import com.revticket.booking.dto.BookingRequest;
import com.revticket.booking.dto.BookingResponse;
import com.revticket.booking.dto.CancellationRequest;
import com.revticket.booking.service.BookingService;
import com.revticket.booking.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        String userId = securityUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(bookingService.createBooking(userId, request));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        String userId = securityUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getUserBookingsByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable String id) {
        return bookingService.getBookingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/request-cancellation")
    public ResponseEntity<BookingResponse> requestCancellation(
            @PathVariable String id,
            @RequestBody(required = false) CancellationRequest request) {
        String reason = request != null && request.getReason() != null ? request.getReason() : "";
        return ResponseEntity.ok(bookingService.requestCancellation(id, reason));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable String id,
            @RequestBody(required = false) String reason) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, reason));
    }

    @GetMapping("/cancellation-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponse>> getCancellationRequests() {
        return ResponseEntity.ok(bookingService.getCancellationRequests());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBooking(@PathVariable String id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/scan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> scanBooking(@PathVariable String id) {
        return ResponseEntity.ok(bookingService.scanBooking(id));
    }

    @PostMapping("/{id}/resign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> resignBooking(
            @PathVariable String id,
            @RequestBody List<String> newSeats) {
        return ResponseEntity.ok(bookingService.resignBooking(id, newSeats));
    }

    @GetMapping("/verify/{id}")
    public ResponseEntity<?> verifyTicket(@PathVariable String id) {
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

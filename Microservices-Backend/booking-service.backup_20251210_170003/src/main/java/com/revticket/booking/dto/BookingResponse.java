package com.revticket.booking.dto;

import com.revticket.booking.entity.Booking;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class BookingResponse {
    String id;
    String userId;
    String movieId;
    String movieTitle;
    String moviePosterUrl;
    String theaterId;
    String theaterName;
    String theaterLocation;
    String showtimeId;
    LocalDateTime showtime;
    String screen;
    Double ticketPrice;
    List<String> seats;
    List<String> seatLabels;
    Double totalAmount;
    LocalDateTime bookingDate;
    Booking.BookingStatus status;
    String customerName;
    String customerEmail;
    String customerPhone;
    String paymentId;
    String qrCode;
    String ticketNumber;
    Double refundAmount;
    LocalDateTime refundDate;
    String cancellationReason;
}

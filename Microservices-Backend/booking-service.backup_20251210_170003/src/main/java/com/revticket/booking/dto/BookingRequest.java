package com.revticket.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingRequest {
    @NotBlank(message = "Movie ID is required")
    private String movieId;

    @NotBlank(message = "Theater ID is required")
    private String theaterId;

    @NotBlank(message = "Showtime ID is required")
    private String showtimeId;

    // User ID - passed from payment service
    private String userId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<String> seats;

    private List<String> seatLabels;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private Double totalAmount;

    @NotNull(message = "Showtime is required")
    private LocalDateTime showtime;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    private String customerEmail;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;
}

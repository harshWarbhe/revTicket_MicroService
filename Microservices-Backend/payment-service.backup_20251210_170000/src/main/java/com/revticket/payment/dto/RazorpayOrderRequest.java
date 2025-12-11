package com.revticket.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RazorpayOrderRequest {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Showtime ID is required")
    private String showtimeId;

    @NotBlank(message = "Currency is required")
    private String currency = "INR";
}

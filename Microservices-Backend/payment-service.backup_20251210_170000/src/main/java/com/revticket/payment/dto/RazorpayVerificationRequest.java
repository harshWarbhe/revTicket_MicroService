package com.revticket.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RazorpayVerificationRequest {
    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;

    @NotBlank(message = "Showtime ID is required")
    private String showtimeId;

    @NotEmpty(message = "Seats are required")
    private List<String> seats;

    private List<String> seatLabels;

    @NotNull(message = "Total amount is required")
    private Double totalAmount;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    private String customerEmail;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;
}

package com.revticket.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "showtime_id", nullable = false)
    private String showtimeId;

    @ElementCollection
    @CollectionTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_id")
    private List<String> seats = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "booking_seat_labels", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_label")
    private List<String> seatLabels = new ArrayList<>();

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "ticket_price_snapshot")
    private Double ticketPriceSnapshot;

    @Column(name = "screen_name")
    private String screenName;

    @Column(name = "payment_method")
    private String paymentMethod;

    @CreationTimestamp
    @Column(name = "booking_date", nullable = false, updatable = false)
    private LocalDateTime bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "ticket_number")
    private String ticketNumber;

    @Column(name = "refund_amount")
    private Double refundAmount;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancellation_requested_at")
    private LocalDateTime cancellationRequestedAt;

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, CANCELLATION_PENDING
    }
}

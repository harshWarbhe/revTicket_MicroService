package com.revticket.payment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    @JsonBackReference("showtime-seats")
    private Showtime showtime;

    @Column(name = "`row`", nullable = false)
    private String row;

    @Column(nullable = false)
    private Integer number;

    @Column(name = "is_booked", nullable = false)
    private Boolean isBooked = false;

    @Column(name = "is_held", nullable = false)
    private Boolean isHeld = false;

    @Column(name = "is_disabled", nullable = false)
    private Boolean isDisabled = false;

    @Column(nullable = false)
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType type;

    @Column(name = "hold_expiry")
    private java.time.LocalDateTime holdExpiry;

    @Column(name = "session_id")
    private String sessionId;

    public enum SeatType {
        REGULAR, PREMIUM, VIP
    }
}


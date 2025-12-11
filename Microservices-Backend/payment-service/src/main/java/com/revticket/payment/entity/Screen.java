package com.revticket.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "screens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Screen {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "`rows`")
    private Integer rows;

    @Column(name = "seats_per_row")
    private Integer seatsPerRow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @Column(name = "is_active")
    private Boolean isActive = true;
}

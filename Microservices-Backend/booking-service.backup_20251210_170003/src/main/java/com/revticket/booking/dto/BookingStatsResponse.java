package com.revticket.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatsResponse {
    private Long totalBookings;
    private Long cancelledBookings;
    private Long bookingsLast7Days;
    private Long bookingsLast30Days;
    private Long totalSeatsBooked;
}

package com.revticket.booking.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShowtimeDTO {
    private String id;
    private String movieId;
    private String theaterId;
    private String screen;
    private LocalDateTime showDateTime;
    private Double ticketPrice;
}

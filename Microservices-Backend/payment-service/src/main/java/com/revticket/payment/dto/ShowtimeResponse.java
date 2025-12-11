package com.revticket.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShowtimeResponse {
    private String id;
    private String movieId;
    private String theaterId;
    private String screen;
    private LocalDateTime showDateTime;
    private Double ticketPrice;
    private Integer totalSeats;
    private Integer availableSeats;
    private String status;
    private MovieSummary movie;
    private TheaterSummary theater;
    private ScreenSummary screenInfo;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MovieSummary {
        private String id;
        private String title;
        private String language;
        private Integer duration;
        private Double rating;
        private String posterUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TheaterSummary {
        private String id;
        private String name;
        private String location;
        private String address;
        private Integer totalScreens;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScreenSummary {
        private String id;
        private String name;
        private Integer totalSeats;
    }
}

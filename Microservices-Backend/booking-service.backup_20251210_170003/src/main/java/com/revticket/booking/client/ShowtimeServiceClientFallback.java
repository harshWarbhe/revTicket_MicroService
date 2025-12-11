package com.revticket.booking.client;

import com.revticket.booking.dto.ShowtimeDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ShowtimeServiceClientFallback implements ShowtimeServiceClient {
    @Override
    public ShowtimeDTO getShowtimeById(String id) {
        ShowtimeDTO fallback = new ShowtimeDTO();
        fallback.setId(id);
        fallback.setMovieId("");
        fallback.setTheaterId("");
        fallback.setScreen("N/A");
        fallback.setShowDateTime(LocalDateTime.now());
        fallback.setTicketPrice(0.0);
        return fallback;
    }
}

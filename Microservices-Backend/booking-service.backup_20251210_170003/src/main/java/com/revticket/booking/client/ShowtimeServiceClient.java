package com.revticket.booking.client;

import com.revticket.booking.dto.ShowtimeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "showtime-service", fallback = ShowtimeServiceClientFallback.class)
public interface ShowtimeServiceClient {
    @GetMapping("/api/showtimes/{id}")
    ShowtimeDTO getShowtimeById(@PathVariable("id") String id);
}

package com.revticket.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "showtime-service")
public interface ShowtimeServiceClient {

    @GetMapping("/api/showtimes/{id}")
    Map<String, Object> getShowtimeById(@PathVariable("id") String id);
}

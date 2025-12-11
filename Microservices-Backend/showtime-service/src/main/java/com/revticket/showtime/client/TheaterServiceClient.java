package com.revticket.showtime.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "theater-service", configuration = com.revticket.showtime.config.FeignConfig.class)
public interface TheaterServiceClient {
    @GetMapping("/api/theaters/{id}")
    Map<String, Object> getTheaterById(@PathVariable String id);

    @GetMapping("/api/screens/{id}")
    Map<String, Object> getScreenById(@PathVariable String id);
}

package com.revticket.booking.client;

import com.revticket.booking.dto.TheaterDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "theater-service", fallback = TheaterServiceClientFallback.class)
public interface TheaterServiceClient {
    @GetMapping("/api/screens/{id}/config")
    Map<String, Object> getScreenConfig(@PathVariable String id);
    
    @GetMapping("/api/theaters/{id}")
    TheaterDTO getTheaterById(@PathVariable("id") String id);
}

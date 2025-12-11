package com.revticket.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "booking-service", configuration = com.revticket.review.config.FeignConfig.class)
public interface BookingServiceClient {
    @GetMapping("/api/bookings/user/{userId}")
    List<Map<String, Object>> getUserBookings(@PathVariable String userId);
}

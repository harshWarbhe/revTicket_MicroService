package com.revticket.dashboard.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "booking-service", configuration = com.revticket.dashboard.config.FeignConfig.class)
public interface BookingServiceClient {

    @GetMapping("/api/admin/bookings/stats")
    Map<String, Object> getBookingStats();

    @GetMapping("/api/bookings/all")
    java.util.List<Object> getAllBookings();
}

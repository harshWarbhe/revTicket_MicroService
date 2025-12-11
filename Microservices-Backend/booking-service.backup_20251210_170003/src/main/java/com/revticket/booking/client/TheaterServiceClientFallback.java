package com.revticket.booking.client;

import com.revticket.booking.dto.TheaterDTO;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TheaterServiceClientFallback implements TheaterServiceClient {
    @Override
    public Map<String, Object> getScreenConfig(String id) {
        return new HashMap<>();
    }

    @Override
    public TheaterDTO getTheaterById(String id) {
        TheaterDTO fallback = new TheaterDTO();
        fallback.setId(id);
        fallback.setName("Theater Information Unavailable");
        fallback.setLocation("");
        fallback.setCity("");
        return fallback;
    }
}

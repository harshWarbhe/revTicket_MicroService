package com.revticket.booking.controller;

import com.revticket.booking.entity.Seat;
import com.revticket.booking.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    @Autowired
    private SeatService seatService;

    @GetMapping("/showtime/{showtimeId}")
    public ResponseEntity<List<Seat>> getSeatsByShowtime(@PathVariable("showtimeId") String showtimeId) {
        return ResponseEntity.ok(seatService.getSeatsByShowtime(showtimeId));
    }

    @PostMapping("/initialize")
    public ResponseEntity<Map<String, String>> initializeSeats(@RequestBody Map<String, Object> request) {
        String showtimeId = (String) request.get("showtimeId");
        String screenId = (String) request.get("screenId");
        
        seatService.initializeSeatsForShowtime(showtimeId, screenId);
        return ResponseEntity.ok(Map.of("message", "Seats initialized successfully"));
    }

    @PostMapping("/hold")
    public ResponseEntity<Map<String, String>> holdSeats(@RequestBody Map<String, Object> request) {
        String showtimeId = (String) request.get("showtimeId");
        @SuppressWarnings("unchecked")
        List<String> seatIds = (List<String>) request.get("seatIds");
        String sessionId = (String) request.get("sessionId");
        
        seatService.holdSeats(showtimeId, seatIds, sessionId);
        return ResponseEntity.ok(Map.of("message", "Seats held successfully"));
    }

    @PostMapping("/release")
    public ResponseEntity<Map<String, String>> releaseSeats(@RequestBody Map<String, Object> request) {
        String showtimeId = (String) request.get("showtimeId");
        @SuppressWarnings("unchecked")
        List<String> seatIds = (List<String>) request.get("seatIds");
        
        seatService.releaseSeats(showtimeId, seatIds);
        return ResponseEntity.ok(Map.of("message", "Seats released successfully"));
    }
}

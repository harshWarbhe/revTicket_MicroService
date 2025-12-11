package com.revticket.booking.service;

import com.revticket.booking.client.TheaterServiceClient;
import com.revticket.booking.entity.Seat;
import com.revticket.booking.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SeatService {

    @Autowired
    private TheaterServiceClient theaterServiceClient;

    @Autowired
    private SeatRepository seatRepository;

    @Transactional
    public List<Seat> getSeatsByShowtime(String showtimeId) {
        List<Seat> seats = seatRepository.findByShowtimeId(showtimeId);

        LocalDateTime now = LocalDateTime.now();
        List<Seat> expiredHolds = new ArrayList<>();
        for (Seat seat : seats) {
            if (Boolean.TRUE.equals(seat.getIsHeld()) &&
                seat.getHoldExpiry() != null &&
                seat.getHoldExpiry().isBefore(now)) {
                seat.setIsHeld(false);
                seat.setHoldExpiry(null);
                seat.setSessionId(null);
                expiredHolds.add(seat);
            }
        }

        if (!expiredHolds.isEmpty()) {
            seatRepository.saveAll(expiredHolds);
        }

        return seats;
    }

    @Transactional
    public void holdSeats(String showtimeId, List<String> seatIds, String sessionId) {
        for (String seatId : seatIds) {
            String safeSeatId = Objects.requireNonNullElse(seatId, "");
            Seat seat = seatRepository.findById(safeSeatId)
                    .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));

            if (seat.getIsBooked()) {
                throw new RuntimeException("Seat " + seatId + " is already booked");
            }

            seat.setIsHeld(true);
            seat.setHoldExpiry(LocalDateTime.now().plusMinutes(10));
            seat.setSessionId(sessionId);
            seatRepository.save(seat);
        }
    }

    @Transactional
    public void releaseSeats(String showtimeId, List<String> seatIds) {
        for (String seatId : seatIds) {
            String safeSeatId = Objects.requireNonNullElse(seatId, "");
            Seat seat = seatRepository.findById(safeSeatId).orElse(null);
            if (seat != null && !seat.getIsBooked()) {
                seat.setIsHeld(false);
                seat.setHoldExpiry(null);
                seat.setSessionId(null);
                seatRepository.save(seat);
            }
        }
    }

    @Transactional
    public void initializeSeatsForShowtime(String showtimeId, String screenId) {
        List<Seat> existingSeats = seatRepository.findByShowtimeId(showtimeId);
        if (!existingSeats.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> screenConfig = theaterServiceClient.getScreenConfig(screenId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> seatMap = (List<Map<String, Object>>) screenConfig.get("seatMap");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categories = (List<Map<String, Object>>) screenConfig.get("categories");

            List<Seat> seats = new ArrayList<>();
            for (Map<String, Object> seatData : seatMap) {
                String status = (String) seatData.get("status");
                if ("disabled".equals(status)) {
                    continue;
                }

                Integer rowNum = (Integer) seatData.get("row");
                Integer col = (Integer) seatData.get("col");
                String categoryId = (String) seatData.get("categoryId");

                Map<String, Object> category = categories.stream()
                        .filter(c -> categoryId.equals(c.get("id")))
                        .findFirst()
                        .orElse(null);

                Double price = category != null ? ((Number) category.get("price")).doubleValue() : 100.0;

                Seat seat = new Seat();
                seat.setShowtimeId(showtimeId);
                seat.setRow(String.valueOf((char) ('A' + rowNum)));
                seat.setNumber(col + 1);
                seat.setPrice(price);
                seat.setType(Seat.SeatType.REGULAR);
                seat.setIsBooked(false);
                seat.setIsHeld(false);
                seat.setIsDisabled(false);
                seats.add(seat);
            }

            seatRepository.saveAll(seats);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize seats: " + e.getMessage());
        }
    }
}

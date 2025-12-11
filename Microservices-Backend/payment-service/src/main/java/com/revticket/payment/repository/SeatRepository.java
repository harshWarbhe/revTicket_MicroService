package com.revticket.payment.repository;

import com.revticket.payment.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, String> {
    List<Seat> findByShowtimeId(String showtimeId);
}

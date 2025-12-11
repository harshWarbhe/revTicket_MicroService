package com.revticket.booking.repository;

import com.revticket.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    
    List<Booking> findByUserId(String userId);
    
    List<Booking> findByShowtimeId(String showtimeId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate BETWEEN :start AND :end")
    Long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0.0) FROM Booking b WHERE b.status = 'CONFIRMED' AND b.bookingDate BETWEEN :start AND :end")
    Double sumRevenueByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'CANCELLED' AND b.bookingDate BETWEEN :start AND :end")
    Long countCancelledByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(b.refundAmount), 0.0) FROM Booking b WHERE b.status = 'CANCELLED' AND b.bookingDate BETWEEN :start AND :end")
    Double sumRefundsByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    Long countByStatus(Booking.BookingStatus status);
}

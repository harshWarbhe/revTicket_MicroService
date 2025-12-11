package com.revticket.payment.repository;

import com.revticket.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByBookingId(String bookingId);
    
    Long countByStatus(Payment.PaymentStatus status);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM Payment p WHERE p.status = 'SUCCESS'")
    Double sumSuccessfulPayments();
    
    @Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.paymentDate >= :startDate")
    Double sumSuccessfulPaymentsAfter(@Param("startDate") LocalDateTime startDate);
}

package com.revticket.payment.service;

import com.revticket.payment.dto.PaymentRequest;
import com.revticket.payment.entity.Booking;
import com.revticket.payment.entity.Payment;
import com.revticket.payment.repository.BookingRepository;
import com.revticket.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Transactional
    public Payment processPayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(Objects.requireNonNullElse(request.getBookingId(), ""))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(Payment.PaymentMethod.valueOf(request.getPaymentMethod()));
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setTransactionId("TXN" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());

        payment = paymentRepository.save(payment);

        // Update booking status
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentId(payment.getId());
        bookingRepository.save(booking);

        return payment;
    }

    public Optional<Payment> getPaymentStatus(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }
}


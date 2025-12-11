package com.revticket.payment.controller;

import com.revticket.payment.dto.PaymentRequest;
import com.revticket.payment.dto.PaymentStatsResponse;
import com.revticket.payment.entity.Payment;
import com.revticket.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> processPayment(@Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "transactionId", payment.getTransactionId()
        ));
    }

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<Map<String, String>> getPaymentStatus(@PathVariable("transactionId") String transactionId) {
        return paymentService.getPaymentStatus(transactionId)
                .map(payment -> ResponseEntity.ok(Map.of("status", payment.getStatus().name())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/payments/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentStatsResponse> getPaymentStats() {
        return ResponseEntity.ok(paymentService.getPaymentStats());
    }
}

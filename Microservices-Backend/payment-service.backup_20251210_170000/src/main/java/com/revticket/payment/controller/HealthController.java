package com.revticket.payment.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/razorpay")
public class HealthController {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("razorpayKeyId", razorpayKeyId);
        response.put("configured", razorpayKeyId != null && !razorpayKeyId.isEmpty());
        return ResponseEntity.ok(response);
    }
}

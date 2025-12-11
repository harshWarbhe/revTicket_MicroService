package com.revticket.payment.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityUtil {

    @Autowired
    private JwtUtil jwtUtil;

    public String getCurrentUserId(Authentication authentication) {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes) {
                HttpServletRequest req = ((ServletRequestAttributes) attrs).getRequest();
                
                // Check X-User-Id header from API Gateway
                String headerUserId = req.getHeader("X-User-Id");
                if (headerUserId != null && !headerUserId.isEmpty()) {
                    return headerUserId;
                }

                // Extract from Authorization header
                String authHeader = req.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    return jwtUtil.extractUserId(token);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract user ID: " + e.getMessage());
        }

        throw new RuntimeException("No authorization header found");
    }
}

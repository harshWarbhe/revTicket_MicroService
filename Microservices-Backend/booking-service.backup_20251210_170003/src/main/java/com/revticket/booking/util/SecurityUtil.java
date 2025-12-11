package com.revticket.booking.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Security Utility for Booking Service
 * Mimics monolithic SecurityUtil but extracts from JWT in Authorization header
 */
@Component
public class SecurityUtil {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get current user ID from JWT token
     * Equivalent to monolithic SecurityUtil.getCurrentUserId(authentication)
     */
    public String getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.extractUserId(token);
        }

        return null;
    }

    /**
     * Get current user role from JWT token
     */
    public String getCurrentUserRole() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.extractRole(token);
        }

        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

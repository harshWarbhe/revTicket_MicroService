package com.revticket.booking.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT Utility for Booking Service
 * Extracts user information from JWT tokens (similar to monolithic
 * SecurityUtil)
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Extract userId from JWT token
     * Equivalent to SecurityUtil.getCurrentUserId() in monolithic
     */
    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extract user role from JWT token
     */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * Extract all claims from JWT token
     */
    private Claims extractClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

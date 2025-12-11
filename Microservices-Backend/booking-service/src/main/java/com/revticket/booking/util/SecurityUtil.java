package com.revticket.booking.util;

import com.revticket.booking.entity.User;
import com.revticket.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityUtil {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public String getCurrentUserId(Authentication authentication) {
        // Try to extract from request headers first
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes) {
                HttpServletRequest req = ((ServletRequestAttributes) attrs).getRequest();
                
                String headerUserId = req.getHeader("X-User-Id");
                if (headerUserId != null && !headerUserId.isEmpty()) {
                    return headerUserId;
                }

                String authHeader = req.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    String userId = jwtUtil.extractUserId(token);
                    if (userId != null && !userId.isEmpty()) {
                        return userId;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting userId from request: " + e.getMessage());
        }

        // Fallback: extract from authentication principal
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                return user.getId();
            }
        }

        throw new RuntimeException("Unable to extract user ID");
    }

    public User getCurrentUser(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

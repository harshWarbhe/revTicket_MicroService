package com.revticket.user.service;

import com.revticket.user.dto.AuthResponse;
import com.revticket.user.dto.LoginRequest;
import com.revticket.user.dto.OAuth2LoginRequest;
import com.revticket.user.dto.SignupRequest;
import com.revticket.user.dto.UserDto;
import com.revticket.user.entity.User;
import com.revticket.user.repository.UserRepository;
import com.revticket.user.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public AuthResponse login(LoginRequest request) {
        try {
            // Get user from database first to check auth provider
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            // Check if user is using OAuth2 - they cannot login with password
            if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
                throw new BadCredentialsException("This account is registered with " +
                        user.getAuthProvider() + ". Please use " + user.getAuthProvider() + " login.");
            }

            // Authenticate user - this will verify password using BCrypt
            // If authentication fails, it will throw AuthenticationException
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            // Normalize role to handle case-insensitive values from database
            String role = normalizeRole(user.getRole());

            // Generate JWT token with normalized role
            String token = jwtUtil.generateToken(user.getEmail(), role, user.getId());
            UserDto userDto = convertToDto(user);

            return new AuthResponse(token, userDto);
        } catch (AuthenticationException e) {
            // Re-throw authentication exceptions with clear message
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Normalize role to handle case-insensitive role values from database
     */
    private String normalizeRole(User.Role role) {
        if (role == null) {
            return "USER";
        }
        // Enum name() already returns uppercase, but handle any case issues
        String roleStr = role.name();
        if (roleStr.equalsIgnoreCase("ADMIN")) {
            return "ADMIN";
        }
        return "USER";
    }

    public AuthResponse signup(SignupRequest request) {
        // Check if user exists with LOCAL auth provider
        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingUser != null) {
            // If user exists with LOCAL provider, reject signup
            if (existingUser.getAuthProvider() == User.AuthProvider.LOCAL) {
                throw new RuntimeException("Email already exists");
            }
            // If user exists with OAuth2 provider, inform them to use OAuth2
            throw new RuntimeException("This email is registered with " + existingUser.getAuthProvider() +
                    ". Please use " + existingUser.getAuthProvider() + " login instead.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setAddress(request.getAddress());
        user.setPreferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "English");
        user.setRole(User.Role.USER);
        user.setAuthProvider(User.AuthProvider.LOCAL); // Set as LOCAL auth

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        UserDto userDto = convertToDto(user);

        return new AuthResponse(token, userDto);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        System.out.println("\n=== PASSWORD RESET TOKEN ===");
        System.out.println("Reset Token: " + resetToken);
        System.out.println("Reset URL: " + frontendUrl + "/auth/reset-password?token=" + resetToken);
        System.out.println("===========================\n");
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public AuthResponse oauth2Login(OAuth2LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    // Create new OAuth2 user
                    User newUser = new User();
                    newUser.setEmail(request.getEmail());
                    newUser.setName(request.getName());
                    // Set a random password for OAuth2 users (they won't use it)
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setRole(User.Role.USER);

                    // Set OAuth2 provider
                    User.AuthProvider provider = User.AuthProvider.GOOGLE; // default
                    if (request.getProvider() != null) {
                        try {
                            provider = User.AuthProvider.valueOf(request.getProvider().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            // Keep default GOOGLE if invalid provider
                        }
                    }
                    newUser.setAuthProvider(provider);
                    newUser.setProfilePictureUrl(request.getPicture());

                    return userRepository.save(newUser);
                });

        // If existing user, verify they're using OAuth2 (not LOCAL)
        if (user.getAuthProvider() == User.AuthProvider.LOCAL) {
            throw new RuntimeException("This email is already registered with password login. " +
                    "Please use your email and password to login.");
        }

        // Update profile picture if changed
        if (request.getPicture() != null && !request.getPicture().equals(user.getProfilePictureUrl())) {
            user.setProfilePictureUrl(request.getPicture());
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        UserDto userDto = convertToDto(user);
        return new AuthResponse(token, userDto);
    }

    private UserDto convertToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getPhone(),
                user.getDateOfBirth(),
                user.getGender(),
                user.getAddress(),
                user.getPreferredLanguage(),
                user.getEmailNotifications(),
                user.getSmsNotifications(),
                user.getPushNotifications(),
                user.getCreatedAt());
    }
}

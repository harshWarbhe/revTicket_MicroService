package com.revticket.payment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.function.Function;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        try {
            String userEmail = extractUsername(jwt);
            if (userEmail != null && isTokenValid(jwt, userEmail)) {
                Collection<GrantedAuthority> authorities = extractAuthorities(jwt, request);
                UserDetails userDetails = User.builder()
                        .username(userEmail)
                        .password("")
                        .authorities(authorities)
                        .build();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            System.err.println("JWT validation error: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private boolean isTokenValid(String token, String userEmail) {
        return userEmail.equals(extractUsername(token)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Collection<GrantedAuthority> extractAuthorities(String token, HttpServletRequest request) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        
        String headerRole = request.getHeader("X-User-Role");
        if (headerRole != null && !headerRole.isEmpty()) {
            String roleStr = headerRole.toUpperCase();
            if (!roleStr.startsWith("ROLE_")) {
                roleStr = "ROLE_" + roleStr;
            }
            authorities.add(new SimpleGrantedAuthority(roleStr));
            return authorities;
        }
        
        try {
            Claims claims = extractAllClaims(token);
            String role = claims.get("role", String.class);
            if (role != null && !role.isEmpty()) {
                String roleStr = role.toUpperCase();
                if (!roleStr.startsWith("ROLE_")) {
                    roleStr = "ROLE_" + roleStr;
                }
                authorities.add(new SimpleGrantedAuthority(roleStr));
            }
        } catch (Exception e) {
            System.err.println("Error extracting authorities: " + e.getMessage());
        }
        return authorities;
    }
}

package com.revticket.showtime.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Feign configuration to forward the Authorization header from incoming
 * requests
 * to outgoing Feign client calls. This ensures JWT tokens are propagated
 * through
 * the service chain, matching the monolithic backend's authentication pattern.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Get the current HTTP request context
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    // Forward ONLY the Authorization header (JWT token)
                    // This matches the monolithic backend's authentication pattern
                    String authorization = request.getHeader("Authorization");
                    if (authorization != null) {
                        template.header("Authorization", authorization);
                    }
                }
            }
        };
    }
}

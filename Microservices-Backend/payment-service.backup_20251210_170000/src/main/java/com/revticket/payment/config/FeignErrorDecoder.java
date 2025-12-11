package com.revticket.payment.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeignErrorDecoder implements ErrorDecoder {
    
    private static final Logger logger = LoggerFactory.getLogger(FeignErrorDecoder.class);
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        logger.error("Feign client error: method={}, status={}, reason={}", 
                methodKey, response.status(), response.reason());
        
        switch (response.status()) {
            case 400:
                return new RuntimeException("Bad request to " + methodKey + ": " + response.reason());
            case 404:
                return new RuntimeException("Resource not found in " + methodKey);
            case 500:
                return new RuntimeException("Internal server error in " + methodKey);
            default:
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}

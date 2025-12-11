package com.revticket.dashboard.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "review-service", configuration = com.revticket.dashboard.config.FeignConfig.class)
public interface ReviewServiceClient {

    @GetMapping("/api/admin/reviews/stats")
    Map<String, Object> getReviewStats();
}

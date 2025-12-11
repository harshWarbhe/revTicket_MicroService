package com.revticket.dashboard.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "user-service", configuration = com.revticket.dashboard.config.FeignConfig.class)
public interface UserServiceClient {

    @GetMapping("/api/admin/users/stats")
    Map<String, Object> getUserStats();
}

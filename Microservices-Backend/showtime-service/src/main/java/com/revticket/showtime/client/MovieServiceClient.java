package com.revticket.showtime.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "movie-service", configuration = com.revticket.showtime.config.FeignConfig.class)
public interface MovieServiceClient {
    @GetMapping("/api/movies/{id}")
    Map<String, Object> getMovieById(@PathVariable String id);
}

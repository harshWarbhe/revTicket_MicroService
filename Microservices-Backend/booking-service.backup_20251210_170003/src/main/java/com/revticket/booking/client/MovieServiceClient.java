package com.revticket.booking.client;

import com.revticket.booking.dto.MovieDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "movie-service", fallback = MovieServiceClientFallback.class)
public interface MovieServiceClient {
    @GetMapping("/api/movies/{id}")
    MovieDTO getMovieById(@PathVariable("id") String id);
}

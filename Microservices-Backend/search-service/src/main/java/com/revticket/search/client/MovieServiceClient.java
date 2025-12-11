package com.revticket.search.client;

import com.revticket.search.dto.MovieSearchDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "movie-service", configuration = com.revticket.search.config.FeignConfig.class)
public interface MovieServiceClient {

    @GetMapping("/api/movies/search")
    List<MovieSearchDTO> searchMovies(@RequestParam String query);
}

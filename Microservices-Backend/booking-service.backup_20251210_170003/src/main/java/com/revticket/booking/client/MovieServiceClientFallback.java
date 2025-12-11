package com.revticket.booking.client;

import com.revticket.booking.dto.MovieDTO;
import org.springframework.stereotype.Component;

@Component
public class MovieServiceClientFallback implements MovieServiceClient {
    @Override
    public MovieDTO getMovieById(String id) {
        MovieDTO fallback = new MovieDTO();
        fallback.setId(id);
        fallback.setTitle("Movie Information Unavailable");
        fallback.setPosterUrl("");
        return fallback;
    }
}

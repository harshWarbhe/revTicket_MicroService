package com.revticket.search.client;

import com.revticket.search.dto.ShowtimeSearchDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "showtime-service", configuration = com.revticket.search.config.FeignConfig.class)
public interface ShowtimeServiceClient {

    @GetMapping("/api/showtimes/search")
    List<ShowtimeSearchDTO> searchShowtimes(@RequestParam String query);
}

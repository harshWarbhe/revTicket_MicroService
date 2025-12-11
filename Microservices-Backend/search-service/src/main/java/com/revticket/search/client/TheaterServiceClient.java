package com.revticket.search.client;

import com.revticket.search.dto.TheaterSearchDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "theater-service", configuration = com.revticket.search.config.FeignConfig.class)
public interface TheaterServiceClient {

    @GetMapping("/api/theaters/search")
    List<TheaterSearchDTO> searchTheaters(@RequestParam String query);
}

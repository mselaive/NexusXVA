package com.nexusxva.tradebooking.api;

import com.nexusxva.tradebooking.application.TradeBookingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trade-bookings")
public class TradeBookingController {

    private final TradeBookingService service;

    public TradeBookingController(TradeBookingService service) {
        this.service = service;
    }

    @GetMapping("/mine")
    public List<TradeBookingResponse> mine(HttpServletRequest request) {
        return service.mine(TradeBookingActorResolver.resolve(request))
                .stream()
                .map(TradeBookingResponse::from)
                .toList();
    }
}


package com.nexusxva.tradebooking.api;

import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.application.TradeBookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/trade-bookings")
public class PortfolioTradeBookingController {

    private final TradeBookingService service;

    public PortfolioTradeBookingController(TradeBookingService service) {
        this.service = service;
    }

    @PostMapping("/european-options")
    public ResponseEntity<TradeBookingResponse> submit(
            @PathVariable UUID portfolioId,
            @Valid @RequestBody CreateEuropeanOptionBookingRequest request,
            HttpServletRequest servletRequest
    ) {
        TradeBookingRequest booking = service.submitEuropeanOption(
                portfolioId,
                request.toCommand(),
                TradeBookingActorResolver.resolve(servletRequest)
        );
        return ResponseEntity
                .created(URI.create("/api/trade-bookings/" + booking.id()))
                .body(TradeBookingResponse.from(booking));
    }
}


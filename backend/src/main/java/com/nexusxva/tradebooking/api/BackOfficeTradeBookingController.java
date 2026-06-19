package com.nexusxva.tradebooking.api;

import com.nexusxva.tradebooking.application.TradeBookingService;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/back-office/trade-bookings")
public class BackOfficeTradeBookingController {

    private final TradeBookingService service;

    public BackOfficeTradeBookingController(TradeBookingService service) {
        this.service = service;
    }

    @GetMapping
    public TradeBookingPageResponse search(
            @RequestParam(required = false) TradeBookingStatus status,
            @RequestParam(required = false) UUID portfolioId,
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return TradeBookingPageResponse.from(service.search(status, portfolioId, symbol, page, size));
    }

    @GetMapping("/{bookingId}")
    public TradeBookingResponse get(@PathVariable UUID bookingId) {
        return TradeBookingResponse.from(service.get(bookingId));
    }

    @PostMapping("/{bookingId}/approve")
    public TradeBookingResponse approve(
            @PathVariable UUID bookingId,
            HttpServletRequest request
    ) {
        return TradeBookingResponse.from(
                service.approve(bookingId, TradeBookingActorResolver.resolve(request))
        );
    }

    @PostMapping("/{bookingId}/reject")
    public TradeBookingResponse reject(
            @PathVariable UUID bookingId,
            @Valid @RequestBody RejectTradeBookingRequest body,
            HttpServletRequest request
    ) {
        return TradeBookingResponse.from(
                service.reject(
                        bookingId,
                        TradeBookingActorResolver.resolve(request),
                        body.rejectionReason()
                )
        );
    }
}

package com.nexusxva.tradebooking.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
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
    private final UserAccessService userAccessService;

    public PortfolioTradeBookingController(TradeBookingService service, UserAccessService userAccessService) {
        this.service = service;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/european-options")
    public ResponseEntity<TradeBookingResponse> submit(
            @PathVariable UUID portfolioId,
            @Valid @RequestBody CreateEuropeanOptionBookingRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_BOOK_TRADES);
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        TradeBookingRequest booking = service.submitEuropeanOption(
                portfolioId,
                request.toCommand(),
                TradeBookingActorResolver.resolve(servletRequest)
        );
        return ResponseEntity
                .created(URI.create("/api/trade-bookings/" + booking.id()))
                .body(TradeBookingResponse.from(booking));
    }

    @PostMapping("/option-strategies")
    public ResponseEntity<TradeBookingResponse> submitStrategy(
            @PathVariable UUID portfolioId,
            @Valid @RequestBody CreateOptionStrategyBookingRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_BOOK_TRADES);
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        TradeBookingRequest booking = service.submitOptionStrategy(
                portfolioId,
                request.toCommand(),
                TradeBookingActorResolver.resolve(servletRequest)
        );
        return ResponseEntity
                .created(URI.create("/api/trade-bookings/" + booking.id()))
                .body(TradeBookingResponse.from(booking));
    }

    @PostMapping("/cash-equities")
    public ResponseEntity<TradeBookingResponse> submitCashEquity(
            @PathVariable UUID portfolioId,
            @Valid @RequestBody CreateCashEquityBookingRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_BOOK_TRADES);
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        TradeBookingRequest booking = service.submitCashEquity(
                portfolioId,
                request.toCommand(),
                TradeBookingActorResolver.resolve(servletRequest)
        );
        return ResponseEntity
                .created(URI.create("/api/trade-bookings/" + booking.id()))
                .body(TradeBookingResponse.from(booking));
    }
}

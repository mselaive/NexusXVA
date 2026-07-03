package com.nexusxva.tradebooking.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
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
    private final AuditService auditService;

    public PortfolioTradeBookingController(
            TradeBookingService service,
            UserAccessService userAccessService,
            AuditService auditService
    ) {
        this.service = service;
        this.userAccessService = userAccessService;
        this.auditService = auditService;
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
        auditBookingSubmitted(servletRequest, booking);
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
        auditBookingSubmitted(servletRequest, booking);
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
        auditBookingSubmitted(servletRequest, booking);
        return ResponseEntity
                .created(URI.create("/api/trade-bookings/" + booking.id()))
                .body(TradeBookingResponse.from(booking));
    }

    private void auditBookingSubmitted(HttpServletRequest request, TradeBookingRequest booking) {
        auditService.record(AuditEventCommand.of(
                "TRADE_BOOKING_SUBMITTED",
                "FRONT_OFFICE",
                "SUBMIT_BOOKING",
                AuditOutcome.SUCCESS,
                currentSession(request),
                request,
                201,
                "TRADE_BOOKING",
                booking.id(),
                "Trade booking submitted for BO validation",
                auditService.metadata(java.util.Map.of(
                        "portfolioId", booking.portfolioId(),
                        "portfolioName", booking.portfolioName(),
                        "instrumentType", booking.instrumentType(),
                        "bookingType", booking.bookingType().name(),
                        "symbol", booking.underlyingSymbol(),
                        "quantity", booking.quantity()
                ))
        ));
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}

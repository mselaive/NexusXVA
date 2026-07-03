package com.nexusxva.tradebooking.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.tradebooking.application.TradeBookingService;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
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
    private final AuditService auditService;

    public BackOfficeTradeBookingController(TradeBookingService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
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
        TradeBookingRequest booking = service.approve(bookingId, TradeBookingActorResolver.resolve(request));
        auditReview(request, booking, "TRADE_BOOKING_APPROVED", "APPROVE_BOOKING", "Trade booking approved");
        return TradeBookingResponse.from(booking);
    }

    @PostMapping("/{bookingId}/reject")
    public TradeBookingResponse reject(
            @PathVariable UUID bookingId,
            @Valid @RequestBody RejectTradeBookingRequest body,
            HttpServletRequest request
    ) {
        TradeBookingRequest booking = service.reject(
                bookingId,
                TradeBookingActorResolver.resolve(request),
                body.rejectionReason()
        );
        auditReview(request, booking, "TRADE_BOOKING_REJECTED", "REJECT_BOOKING", "Trade booking rejected");
        return TradeBookingResponse.from(booking);
    }

    private void auditReview(HttpServletRequest request, TradeBookingRequest booking, String eventType, String action, String message) {
        auditService.record(AuditEventCommand.of(
                eventType,
                "BACK_OFFICE",
                action,
                AuditOutcome.SUCCESS,
                currentSession(request),
                request,
                200,
                "TRADE_BOOKING",
                booking.id(),
                message,
                auditService.metadata(java.util.Map.of(
                        "portfolioId", booking.portfolioId(),
                        "status", booking.status().name(),
                        "symbol", booking.underlyingSymbol(),
                        "confirmedPositionIds", booking.confirmedPositionIds()
                ))
        ));
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}

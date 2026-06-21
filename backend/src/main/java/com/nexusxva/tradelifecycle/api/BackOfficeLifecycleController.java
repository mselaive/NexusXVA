package com.nexusxva.tradelifecycle.api;

import com.nexusxva.tradebooking.api.TradeBookingActorResolver;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
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
@RequestMapping("/api/back-office/lifecycle-requests")
public class BackOfficeLifecycleController {

    private final TradeLifecycleService service;

    public BackOfficeLifecycleController(TradeLifecycleService service) {
        this.service = service;
    }

    @GetMapping
    public TradeLifecyclePageResponse search(
            @RequestParam(required = false) TradeLifecycleRequestStatus status,
            @RequestParam(required = false) UUID portfolioId,
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return TradeLifecyclePageResponse.from(service.search(status, portfolioId, symbol, page, size));
    }

    @GetMapping("/{requestId}")
    public TradeLifecycleResponse get(@PathVariable UUID requestId) {
        return TradeLifecycleResponse.from(service.get(requestId));
    }

    @PostMapping("/{requestId}/approve")
    public TradeLifecycleResponse approve(@PathVariable UUID requestId, HttpServletRequest request) {
        return TradeLifecycleResponse.from(
                service.approve(requestId, TradeBookingActorResolver.resolve(request))
        );
    }

    @PostMapping("/{requestId}/reject")
    public TradeLifecycleResponse reject(
            @PathVariable UUID requestId,
            @Valid @RequestBody RejectLifecycleRequest body,
            HttpServletRequest request
    ) {
        return TradeLifecycleResponse.from(
                service.reject(requestId, TradeBookingActorResolver.resolve(request), body.rejectionReason())
        );
    }
}

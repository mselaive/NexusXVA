package com.nexusxva.tradelifecycle.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.tradebooking.api.TradeBookingActorResolver;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office/lifecycle")
public class FrontOfficeLifecycleController {

    private final TradeLifecycleService service;
    private final UserAccessService userAccessService;

    public FrontOfficeLifecycleController(TradeLifecycleService service, UserAccessService userAccessService) {
        this.service = service;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/positions/{positionId}/amend")
    public TradeLifecycleResponse amend(
            @PathVariable UUID positionId,
            @Valid @RequestBody AmendPositionRequest body,
            HttpServletRequest request
    ) {
        userAccessService.requireFeature(request, FeaturePermissionCode.FO_REQUEST_LIFECYCLE);
        EuropeanOptionPosition position = service.position(positionId);
        userAccessService.requirePortfolioAccess(request, position.portfolioId());
        return TradeLifecycleResponse.from(
                service.submitAmend(positionId, body.toCommand(), TradeBookingActorResolver.resolve(request))
        );
    }

    @PostMapping("/positions/{positionId}/cancel")
    public TradeLifecycleResponse cancel(@PathVariable UUID positionId, HttpServletRequest request) {
        userAccessService.requireFeature(request, FeaturePermissionCode.FO_REQUEST_LIFECYCLE);
        EuropeanOptionPosition position = service.position(positionId);
        userAccessService.requirePortfolioAccess(request, position.portfolioId());
        return TradeLifecycleResponse.from(
                service.submitCancel(positionId, TradeBookingActorResolver.resolve(request))
        );
    }

    @GetMapping("/mine")
    public List<TradeLifecycleResponse> mine(HttpServletRequest request) {
        return service.mine(TradeBookingActorResolver.resolve(request))
                .stream()
                .map(TradeLifecycleResponse::from)
                .toList();
    }
}

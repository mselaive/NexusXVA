package com.nexusxva.frontoffice.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.frontoffice.application.FrontOfficeDeltaHedgeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office/delta-hedge")
public class FrontOfficeDeltaHedgeController {

    private final FrontOfficeDeltaHedgeService service;
    private final UserAccessService userAccessService;

    public FrontOfficeDeltaHedgeController(
            FrontOfficeDeltaHedgeService service,
            UserAccessService userAccessService
    ) {
        this.service = service;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/european-options")
    public DeltaHedgeAnalysisResponse run(
            @Valid @RequestBody DeltaHedgeAnalysisRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_RUN_DELTA_HEDGE);
        userAccessService.requirePortfolioAccess(servletRequest, request.portfolioId());
        return DeltaHedgeAnalysisResponse.from(service.run(
                request.portfolioId(),
                request.valuationDate(),
                request.targetDeltaBySymbol()
        ));
    }
}

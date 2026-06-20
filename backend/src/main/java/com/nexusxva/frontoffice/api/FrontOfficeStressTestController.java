package com.nexusxva.frontoffice.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.frontoffice.application.FrontOfficeStressTestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office/stress-tests")
public class FrontOfficeStressTestController {

    private final FrontOfficeStressTestService service;
    private final UserAccessService userAccessService;

    public FrontOfficeStressTestController(
            FrontOfficeStressTestService service,
            UserAccessService userAccessService
    ) {
        this.service = service;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/european-options")
    public FrontOfficeStressTestResponse run(
            @Valid @RequestBody FrontOfficeStressTestRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_RUN_STRESS_TEST);
        userAccessService.requirePortfolioAccess(servletRequest, request.portfolioId());
        return FrontOfficeStressTestResponse.from(service.run(
                request.portfolioId(),
                request.valuationDate(),
                request.hypotheticalTradeCommand(),
                request.scenarioCommands()
        ));
    }
}

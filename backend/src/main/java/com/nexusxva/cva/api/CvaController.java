package com.nexusxva.cva.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.cva.application.CvaCalculationService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class CvaController {

    private final CvaCalculationService cvaCalculationService;
    private final UserAccessService userAccessService;

    public CvaController(CvaCalculationService cvaCalculationService, UserAccessService userAccessService) {
        this.cvaCalculationService = cvaCalculationService;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/cva")
    public CvaCalculationResponse calculateCva(
            @Valid @RequestBody CvaCalculationRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_RUN_CVA);
        userAccessService.requirePortfolioAccess(servletRequest, request.portfolioId());
        return CvaCalculationResponse.from(cvaCalculationService.calculate(request.toCommand()));
    }
}

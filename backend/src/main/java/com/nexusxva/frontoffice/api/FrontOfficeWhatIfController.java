package com.nexusxva.frontoffice.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.frontoffice.application.FrontOfficeWhatIfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office/what-if")
public class FrontOfficeWhatIfController {

    private final FrontOfficeWhatIfService service;
    private final UserAccessService userAccessService;

    public FrontOfficeWhatIfController(FrontOfficeWhatIfService service, UserAccessService userAccessService) {
        this.service = service;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/european-option")
    public FrontOfficeWhatIfResponse run(
            @Valid @RequestBody FrontOfficeWhatIfRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_RUN_WHAT_IF);
        userAccessService.requirePortfolioAccess(servletRequest, request.portfolioId());
        return FrontOfficeWhatIfResponse.from(
                service.run(request.portfolioId(), request.valuationDate(), request.trade().toCommand())
        );
    }
}

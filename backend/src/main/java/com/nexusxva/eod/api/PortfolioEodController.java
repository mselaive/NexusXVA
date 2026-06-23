package com.nexusxva.eod.api;

import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.eod.application.PortfolioEodService;
import com.nexusxva.eod.application.PortfolioDailyPnlService;
import com.nexusxva.shared.error.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/eod")
public class PortfolioEodController {

    private final PortfolioEodService service;
    private final PortfolioDailyPnlService pnlService;
    private final UserAccessService userAccessService;

    public PortfolioEodController(
            PortfolioEodService service,
            PortfolioDailyPnlService pnlService,
            UserAccessService userAccessService
    ) {
        this.service = service;
        this.pnlService = pnlService;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/pnl")
    public PortfolioDailyPnlResponse dailyPnl(
            @PathVariable UUID portfolioId,
            @RequestBody(required = false) CapturePortfolioEodRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        LocalDate valuationDate = request == null ? null : request.businessDate();
        return PortfolioDailyPnlResponse.from(pnlService.calculate(portfolioId, valuationDate));
    }

    @GetMapping("/latest")
    public PortfolioEodSnapshotResponse latest(
            @PathVariable UUID portfolioId,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        return service.latest(portfolioId)
                .map(PortfolioEodSnapshotResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("EOD snapshot not found"));
    }

    @GetMapping
    public List<PortfolioEodSnapshotResponse> history(
            @PathVariable UUID portfolioId,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        return service.history(portfolioId, limit).stream()
                .map(PortfolioEodSnapshotResponse::from)
                .toList();
    }
}

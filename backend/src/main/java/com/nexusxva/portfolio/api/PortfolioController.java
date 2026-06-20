package com.nexusxva.portfolio.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.portfolio.domain.Portfolio;

import jakarta.validation.Valid;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserAccessService userAccessService;

    public PortfolioController(PortfolioService portfolioService, UserAccessService userAccessService) {
        this.portfolioService = portfolioService;
        this.userAccessService = userAccessService;
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_CREATE_PORTFOLIOS);
        Portfolio portfolio = portfolioService.createPortfolio(request.toCommand());
        userAccessService.grantCreatedPortfolioIfNeeded(servletRequest, portfolio.id());

        return ResponseEntity
                .created(URI.create("/api/portfolios/" + portfolio.id()))
                .body(PortfolioResponse.from(portfolio));
    }

    @GetMapping
    public List<PortfolioSummaryResponse> listPortfolios(HttpServletRequest servletRequest) {
        return userAccessService.filterVisiblePortfolios(servletRequest, portfolioService.listPortfolios())
                .stream()
                .map(PortfolioSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{portfolioId}")
    public PortfolioResponse getPortfolio(@PathVariable UUID portfolioId, HttpServletRequest servletRequest) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        return PortfolioResponse.from(portfolioService.getPortfolio(portfolioId));
    }

    @PatchMapping("/{portfolioId}")
    public PortfolioResponse updatePortfolio(
            @PathVariable UUID portfolioId,
            @Valid @RequestBody UpdatePortfolioRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        return PortfolioResponse.from(portfolioService.updatePortfolio(portfolioId, request.toCommand()));
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable UUID portfolioId, HttpServletRequest servletRequest) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        portfolioService.deletePortfolio(portfolioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{portfolioId}/instruments/{positionId}")
    public EuropeanOptionPositionResponse getInstrument(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        return EuropeanOptionPositionResponse.from(
                portfolioService.getEuropeanOptionPosition(portfolioId, positionId)
        );
    }

    @GetMapping("/{portfolioId}/instruments")
    public List<EuropeanOptionPositionResponse> listInstruments(
            @PathVariable UUID portfolioId,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        return portfolioService.listEuropeanOptionPositions(portfolioId)
                .stream()
                .map(EuropeanOptionPositionResponse::from)
                .toList();
    }

}

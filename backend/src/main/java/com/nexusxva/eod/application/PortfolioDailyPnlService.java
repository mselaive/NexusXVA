package com.nexusxva.eod.application;

import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import com.nexusxva.eod.domain.PositionEodSnapshot;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.portfolio.application.PortfolioPositionPricingResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioDailyPnlService {

    private final PortfolioEodStore eodStore;
    private final PortfolioBlackScholesPricingService pricingService;

    public PortfolioDailyPnlService(
            PortfolioEodStore eodStore,
            PortfolioBlackScholesPricingService pricingService
    ) {
        this.eodStore = eodStore;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public PortfolioDailyPnl calculate(UUID portfolioId, LocalDate valuationDate) {
        PortfolioBlackScholesPricingResult pricing = pricingService.price(portfolioId, valuationDate);
        PortfolioEodSnapshot previousEod = eodStore.latest(portfolioId).orElse(null);
        Map<UUID, PositionEodSnapshot> previousByPosition = previousEod == null
                ? Map.of()
                : previousEod.positions().stream()
                        .collect(Collectors.toMap(PositionEodSnapshot::positionId, Function.identity()));

        double totalDailyPnl = 0.0;
        int unavailable = 0;
        ArrayList<PositionDailyPnl> positions = new ArrayList<>();

        for (PortfolioPositionPricingResult position : pricing.positions()) {
            PositionEodSnapshot prior = previousByPosition.get(position.positionId());
            Double referenceValue;
            String method;
            if (prior != null) {
                referenceValue = prior.marketValue();
                method = "PRIOR_EOD";
            } else if (position.tradeValue() != null) {
                referenceValue = position.tradeValue();
                method = "EXECUTION";
            } else {
                referenceValue = null;
                method = "UNAVAILABLE";
            }
            Double dailyPnl = referenceValue == null ? null : position.positionPrice() - referenceValue;
            if (dailyPnl == null) {
                unavailable++;
            } else {
                totalDailyPnl += dailyPnl;
            }
            positions.add(new PositionDailyPnl(
                    position.positionId(),
                    position.underlyingSymbol(),
                    position.positionPrice(),
                    referenceValue,
                    dailyPnl,
                    method
            ));
        }

        return new PortfolioDailyPnl(
                portfolioId,
                pricing.valuationDate(),
                previousEod == null ? null : previousEod.businessDate(),
                pricing.baseCurrency(),
                pricing.totalPrice(),
                totalDailyPnl,
                unavailable,
                positions
        );
    }
}

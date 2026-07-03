package com.nexusxva.eod.application;

import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import com.nexusxva.eod.domain.PositionEodSnapshot;
import com.nexusxva.portfolio.application.CashEquityPositionPricingResult;
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
        double totalSinceTradePnl = 0.0;
        double optionDailyPnl = 0.0;
        double cashEquityDailyPnl = 0.0;
        double optionSinceTradePnl = 0.0;
        double cashEquitySinceTradePnl = 0.0;
        int unavailable = 0;
        int withoutExecutionPrice = 0;
        ArrayList<PositionDailyPnl> positions = new ArrayList<>();

        for (PortfolioPositionPricingResult position : pricing.positions()) {
            PositionDailyPnl pnl = pnlFor(
                    "EUROPEAN_OPTION",
                    position.positionId(),
                    position.underlyingSymbol(),
                    position.positionPrice(),
                    position.tradeValue(),
                    position.unrealizedPnl(),
                    previousByPosition
            );
            positions.add(pnl);
            if (pnl.dailyPnl() == null) {
                unavailable++;
            } else {
                totalDailyPnl += pnl.dailyPnl();
                optionDailyPnl += pnl.dailyPnl();
            }
            if (pnl.sinceTradePnl() == null) {
                withoutExecutionPrice++;
            } else {
                totalSinceTradePnl += pnl.sinceTradePnl();
                optionSinceTradePnl += pnl.sinceTradePnl();
            }
        }

        for (CashEquityPositionPricingResult position : pricing.cashEquityPositions()) {
            PositionDailyPnl pnl = pnlFor(
                    "CASH_EQUITY",
                    position.positionId(),
                    position.underlyingSymbol(),
                    position.marketValue(),
                    position.tradeValue(),
                    position.unrealizedPnl(),
                    previousByPosition
            );
            positions.add(pnl);
            if (pnl.dailyPnl() == null) {
                unavailable++;
            } else {
                totalDailyPnl += pnl.dailyPnl();
                cashEquityDailyPnl += pnl.dailyPnl();
            }
            if (pnl.sinceTradePnl() == null) {
                withoutExecutionPrice++;
            } else {
                totalSinceTradePnl += pnl.sinceTradePnl();
                cashEquitySinceTradePnl += pnl.sinceTradePnl();
            }
        }

        return new PortfolioDailyPnl(
                portfolioId,
                pricing.valuationDate(),
                previousEod == null ? null : previousEod.businessDate(),
                pricing.baseCurrency(),
                pricing.totalPrice(),
                totalDailyPnl,
                totalSinceTradePnl,
                optionDailyPnl,
                cashEquityDailyPnl,
                optionSinceTradePnl,
                cashEquitySinceTradePnl,
                unavailable,
                withoutExecutionPrice,
                positions
        );
    }

    private PositionDailyPnl pnlFor(
            String instrumentType,
            UUID positionId,
            String underlyingSymbol,
            double currentMarketValue,
            Double tradeValue,
            Double sinceTradePnl,
            Map<UUID, PositionEodSnapshot> previousByPosition
    ) {
        PositionEodSnapshot prior = previousByPosition.get(positionId);
        Double referenceValue;
        String method;
        if (prior != null) {
            referenceValue = prior.marketValue();
            method = "PRIOR_EOD";
        } else if (tradeValue != null) {
            referenceValue = tradeValue;
            method = "EXECUTION";
        } else {
            referenceValue = null;
            method = "UNAVAILABLE";
        }
        Double dailyPnl = referenceValue == null ? null : currentMarketValue - referenceValue;
        return new PositionDailyPnl(
                positionId,
                instrumentType,
                underlyingSymbol,
                currentMarketValue,
                referenceValue,
                dailyPnl,
                tradeValue,
                sinceTradePnl,
                method
        );
    }
}

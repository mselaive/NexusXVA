package com.nexusxva.marketrisk.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.nexusxva.marketdata.application.FxRateService;
import com.nexusxva.marketdata.application.HistoricalMarketDataGateway;
import com.nexusxva.marketdata.application.MarketDataPricingInputService;
import com.nexusxva.marketdata.domain.FxRate;
import com.nexusxva.marketdata.domain.HistoricalPriceSeries;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.portfolio.domain.CashEquityPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.portfolio.domain.PositionLifecycleStatus;
import com.nexusxva.pricing.application.EuropeanOptionPricingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistoricalVarServiceTest {
    @Mock PortfolioStore store;
    @Mock HistoricalMarketDataGateway history;
    @Mock MarketDataPricingInputService inputs;
    @Mock FxRateService fx;
    private HistoricalVarService service;
    private final UUID portfolioId=UUID.randomUUID();
    private final LocalDate valuationDate=LocalDate.of(2026,8,2);

    @BeforeEach void setUp(){ service=new HistoricalVarService(store,history,inputs,fx,new EuropeanOptionPricingService()); }

    @Test void calculatesVarAndExpectedShortfallFromAlignedReturns(){
        configure(251);
        HistoricalVarResult result=service.calculate(portfolioId,valuationDate);
        assertThat(result.model()).isEqualTo(HistoricalVarService.MODEL);
        assertThat(result.observations()).isEqualTo(250);
        assertThat(result.valueAtRisk()).isPositive();
        assertThat(result.expectedShortfall()).isGreaterThanOrEqualTo(result.valueAtRisk());
        assertThat(result.worstScenarios()).hasSize(10);
        assertThat(result.varScenarioContributions()).extracting(HistoricalVarResult.SymbolContribution::symbol).containsExactly("AAPL");
        assertThat(result.unmodeledRiskFactors()).containsExactly("VOLATILITY","RATES","DIVIDEND_YIELD","FX");
    }

    @Test void rejectsInsufficientAlignedHistory(){
        configure(240);
        assertThatThrownBy(()->service.calculate(portfolioId,valuationDate))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least 250 aligned returns");
    }

    @Test void rejectsCurrencyMismatchBetweenHistoryAndCurrentInputs(){
        configure(251);
        Instant now=Instant.parse("2026-08-02T12:00:00Z");
        when(inputs.europeanOptionPricingInput("AAPL",valuationDate.plusYears(1)))
                .thenReturn(new MarketDataPricingInput("AAPL",200,.2,.04,0,"EUR",now,"FIXTURE",false));

        assertThatThrownBy(()->service.calculate(portfolioId,valuationDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currencies differ for symbols: AAPL");
    }

    private void configure(int closes){
        Instant now=Instant.parse("2026-08-02T12:00:00Z");
        Portfolio portfolio=new Portfolio(portfolioId,"Risk Book",null,"USD",now,now,List.of());
        CashEquityPosition position=new CashEquityPosition(UUID.randomUUID(),portfolioId,"AAPL",BigDecimal.valueOf(100),
                BigDecimal.valueOf(190), PositionLifecycleStatus.ACTIVE,now,now);
        when(store.findPortfolio(portfolioId)).thenReturn(Optional.of(portfolio));
        when(store.findActiveEuropeanOptionPositions(portfolioId)).thenReturn(List.of());
        when(store.findActiveCashEquityPositions(portfolioId)).thenReturn(List.of(position));
        List<HistoricalPriceSeries.DailyClose> bars=new ArrayList<>();
        double price=100;
        for(int i=0;i<closes;i++){
            double shock=(i%50==0)?-.08:(i%17==0)?.025:.001;
            price*=Math.exp(shock);
            bars.add(new HistoricalPriceSeries.DailyClose(LocalDate.of(2025,1,1).plusDays(i),price));
        }
        when(history.dailyCloses(java.util.Set.of("AAPL"),260)).thenReturn(List.of(new HistoricalPriceSeries("AAPL","USD",false,now,"FIXTURE",bars)));
        lenient().when(inputs.europeanOptionPricingInput("AAPL",valuationDate.plusYears(1))).thenReturn(new MarketDataPricingInput("AAPL",200,.2,.04,0,"USD",now,"FIXTURE",false));
        lenient().when(fx.rate("USD","USD")).thenReturn(new FxRate("USD","USD",1,now,"IDENTITY",false));
    }
}

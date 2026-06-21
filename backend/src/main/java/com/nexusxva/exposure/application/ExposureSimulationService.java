package com.nexusxva.exposure.application;

import com.nexusxva.exposure.domain.ExposureAggregator;
import com.nexusxva.exposure.domain.ExposurePoint;
import com.nexusxva.marketdata.application.MarketDataPricingInputService;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.pricing.application.EuropeanOptionPricingService;
import com.nexusxva.pricing.domain.BlackScholesInput;
import com.nexusxva.pricing.domain.BlackScholesResult;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.simulation.domain.GbmParameters;
import com.nexusxva.simulation.domain.GbmPathGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExposureSimulationService {

    private static final String MODEL = "GBM_BLACK_SCHOLES_EXPOSURE_V1";
    private static final String SUPPORTED_BASE_CURRENCY = "USD";

    private final PortfolioStore portfolioStore;
    private final MarketDataPricingInputService marketDataPricingInputService;
    private final EuropeanOptionPricingService pricingService;
    private final GbmPathGenerator pathGenerator;
    private final ExposureAggregator exposureAggregator;

    @Autowired
    public ExposureSimulationService(
            PortfolioStore portfolioStore,
            MarketDataPricingInputService marketDataPricingInputService,
            EuropeanOptionPricingService pricingService
    ) {
        this(
                portfolioStore,
                marketDataPricingInputService,
                pricingService,
                new GbmPathGenerator(),
                new ExposureAggregator()
        );
    }

    private ExposureSimulationService(
            PortfolioStore portfolioStore,
            MarketDataPricingInputService marketDataPricingInputService,
            EuropeanOptionPricingService pricingService,
            GbmPathGenerator pathGenerator,
            ExposureAggregator exposureAggregator
    ) {
        this.portfolioStore = portfolioStore;
        this.marketDataPricingInputService = marketDataPricingInputService;
        this.pricingService = pricingService;
        this.pathGenerator = pathGenerator;
        this.exposureAggregator = exposureAggregator;
    }

    @Transactional(readOnly = true)
    public ExposureSimulationResult simulate(ExposureSimulationCommand command) {
        Portfolio portfolio = portfolioStore.findPortfolio(command.portfolioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
        if (!SUPPORTED_BASE_CURRENCY.equals(portfolio.baseCurrency())) {
            throw new IllegalArgumentException("Exposure simulation V1 supports USD baseCurrency only");
        }

        List<EuropeanOptionPosition> priceablePositions = portfolioStore
                .findActiveEuropeanOptionPositions(command.portfolioId())
                .stream()
                .filter(position -> position.maturityDate().isAfter(command.valuationDate()))
                .toList();

        List<LocalDate> exposureDates = exposureDates(command.valuationDate(), command.horizonDays(), command.timeSteps());
        if (priceablePositions.isEmpty()) {
            double[][] zeroPortfolioValues = new double[command.paths()][command.timeSteps()];
            List<ExposurePoint> points = exposureAggregator.aggregate(
                    exposureDates,
                    zeroPortfolioValues,
                    command.pfeConfidenceLevel()
            );
            return result(command, points);
        }

        Map<String, MarketDataPricingInput> marketDataBySymbol = marketDataBySymbol(priceablePositions);
        Map<String, double[][]> pathsBySymbol = pathsBySymbol(command, marketDataBySymbol);
        double[][] portfolioValues = portfolioValues(command, exposureDates, priceablePositions, marketDataBySymbol, pathsBySymbol);
        List<ExposurePoint> points = exposureAggregator.aggregate(
                exposureDates,
                portfolioValues,
                command.pfeConfidenceLevel()
        );
        return result(command, points);
    }

    private ExposureSimulationResult result(ExposureSimulationCommand command, List<ExposurePoint> points) {
        return new ExposureSimulationResult(
                command.portfolioId(),
                command.valuationDate(),
                MODEL,
                command.paths(),
                command.timeSteps(),
                command.pfeConfidenceLevel(),
                points
        );
    }

    private List<LocalDate> exposureDates(LocalDate valuationDate, int horizonDays, int timeSteps) {
        return java.util.stream.IntStream.rangeClosed(1, timeSteps)
                .mapToObj(step -> valuationDate.plusDays(Math.round((double) horizonDays * step / timeSteps)))
                .toList();
    }

    private Map<String, MarketDataPricingInput> marketDataBySymbol(List<EuropeanOptionPosition> positions) {
        Map<String, LocalDate> maxMaturityBySymbol = positions.stream()
                .collect(Collectors.toMap(
                        EuropeanOptionPosition::underlyingSymbol,
                        EuropeanOptionPosition::maturityDate,
                        (left, right) -> Comparator.<LocalDate>naturalOrder().compare(left, right) >= 0 ? left : right
                ));

        Map<String, MarketDataPricingInput> marketData = new HashMap<>();
        for (Map.Entry<String, LocalDate> entry : maxMaturityBySymbol.entrySet()) {
            MarketDataPricingInput input = marketDataPricingInputService.europeanOptionPricingInput(
                    entry.getKey(),
                    entry.getValue()
            );
            if (!SUPPORTED_BASE_CURRENCY.equals(input.currency())) {
                throw new IllegalArgumentException("Exposure simulation V1 supports USD market data only");
            }
            marketData.put(entry.getKey(), input);
        }
        return Map.copyOf(marketData);
    }

    private Map<String, double[][]> pathsBySymbol(
            ExposureSimulationCommand command,
            Map<String, MarketDataPricingInput> marketDataBySymbol
    ) {
        double horizonYears = command.horizonDays() / 365.0;
        Map<String, double[][]> paths = new HashMap<>();
        for (Map.Entry<String, MarketDataPricingInput> entry : marketDataBySymbol.entrySet()) {
            MarketDataPricingInput input = entry.getValue();
            paths.put(entry.getKey(), pathGenerator.generate(
                    new GbmParameters(
                            input.spot(),
                            input.riskFreeRate(),
                            input.dividendYield(),
                            input.volatility()
                    ),
                    command.paths(),
                    command.timeSteps(),
                    horizonYears,
                    command.seed() ^ entry.getKey().hashCode()
            ));
        }
        return Map.copyOf(paths);
    }

    private double[][] portfolioValues(
            ExposureSimulationCommand command,
            List<LocalDate> exposureDates,
            List<EuropeanOptionPosition> positions,
            Map<String, MarketDataPricingInput> marketDataBySymbol,
            Map<String, double[][]> pathsBySymbol
    ) {
        double[][] portfolioValues = new double[command.paths()][command.timeSteps()];
        for (int path = 0; path < command.paths(); path++) {
            for (int step = 0; step < command.timeSteps(); step++) {
                LocalDate exposureDate = exposureDates.get(step);
                portfolioValues[path][step] = portfolioValue(path, step + 1, exposureDate, positions, marketDataBySymbol, pathsBySymbol);
            }
        }
        return portfolioValues;
    }

    private double portfolioValue(
            int path,
            int pathStep,
            LocalDate exposureDate,
            List<EuropeanOptionPosition> positions,
            Map<String, MarketDataPricingInput> marketDataBySymbol,
            Map<String, double[][]> pathsBySymbol
    ) {
        double value = 0.0;
        for (EuropeanOptionPosition position : positions) {
            if (!position.maturityDate().isAfter(exposureDate)) {
                continue;
            }
            MarketDataPricingInput input = marketDataBySymbol.get(position.underlyingSymbol());
            double simulatedSpot = pathsBySymbol.get(position.underlyingSymbol())[path][pathStep];
            double timeToMaturityYears = ChronoUnit.DAYS.between(exposureDate, position.maturityDate()) / 365.0;
            BlackScholesResult unitResult = pricingService.priceWithBlackScholes(new BlackScholesInput(
                    position.optionType(),
                    simulatedSpot,
                    position.strike().doubleValue(),
                    timeToMaturityYears,
                    input.riskFreeRate(),
                    input.volatility(),
                    input.dividendYield()
            ));
            value += unitResult.price() * position.quantity().doubleValue();
        }
        return value;
    }
}

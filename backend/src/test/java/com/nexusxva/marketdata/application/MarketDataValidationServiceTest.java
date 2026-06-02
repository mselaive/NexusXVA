package com.nexusxva.marketdata.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexusxva.marketdata.domain.MarketInstrument;
import com.nexusxva.shared.error.ServiceUnavailableException;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class MarketDataValidationServiceTest {

    @Test
    void doesNothingWhenValidationIsDisabled() {
        MarketDataValidationProperties properties = new MarketDataValidationProperties();
        properties.setEnabled(false);
        MarketDataInstrumentGateway gateway = symbol -> {
            throw new AssertionError("gateway should not be called when validation is disabled");
        };

        MarketDataValidationService service = new MarketDataValidationService(properties, gateway);

        assertThatCode(() -> service.validateUnderlyingSymbol("FAKE"))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsKnownActiveInstrument() {
        MarketDataValidationService service = enabledService(symbol -> Optional.of(
                new MarketInstrument("AAPL", true, "Apple Inc.", "EQUITY", "USD")
        ));

        assertThatCode(() -> service.validateUnderlyingSymbol("AAPL"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownInstrument() {
        MarketDataValidationService service = enabledService(symbol -> Optional.empty());

        assertThatThrownBy(() -> service.validateUnderlyingSymbol("FAKE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown underlyingSymbol");
    }

    @Test
    void rejectsInactiveInstrument() {
        MarketDataValidationService service = enabledService(symbol -> Optional.of(
                new MarketInstrument("AAPL", false, "Apple Inc.", "EQUITY", "USD")
        ));

        assertThatThrownBy(() -> service.validateUnderlyingSymbol("AAPL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown underlyingSymbol");
    }

    @Test
    void propagatesMarketDataServiceUnavailability() {
        MarketDataValidationService service = enabledService(symbol -> {
            throw new ServiceUnavailableException("Market data service unavailable");
        });

        assertThatThrownBy(() -> service.validateUnderlyingSymbol("AAPL"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service unavailable");
    }

    private MarketDataValidationService enabledService(MarketDataInstrumentGateway gateway) {
        MarketDataValidationProperties properties = new MarketDataValidationProperties();
        properties.setEnabled(true);
        return new MarketDataValidationService(properties, gateway);
    }
}

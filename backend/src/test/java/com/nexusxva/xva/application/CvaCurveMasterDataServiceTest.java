package com.nexusxva.xva.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexusxva.marketdata.application.MarketDataCurveGateway;
import com.nexusxva.marketdata.domain.MarketCreditCurve;
import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.xva.domain.Counterparty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CvaCurveMasterDataServiceTest {

    private final CvaCurveStore store = mock(CvaCurveStore.class);
    private final XvaStore xvaStore = mock(XvaStore.class);
    private final MarketDataCurveGateway gateway = mock(MarketDataCurveGateway.class);
    private final CvaCurveMasterDataService service = new CvaCurveMasterDataService(store, xvaStore, gateway);
    private final UUID counterpartyId = UUID.randomUUID();
    private final LocalDate valuationDate = LocalDate.parse("2026-08-02");

    @BeforeEach
    void counterpartyExists() {
        when(xvaStore.findCounterparty(counterpartyId)).thenReturn(Optional.of(
                new Counterparty(counterpartyId, "Demo Prime Broker", "DPB-001", "A+", true, Instant.now(), Instant.now())));
    }

    @Test
    void importsRatingProxyAsInactiveMarketDataDraft() {
        when(gateway.getCreditCurve("A+", "USD", valuationDate, 0.40)).thenReturn(marketCurve(false));

        service.importMarketCreditCurve(counterpartyId, valuationDate, 0.40, null, false);

        ArgumentCaptor<SaveCreditCurveCommand> captor = ArgumentCaptor.forClass(SaveCreditCurveCommand.class);
        verify(store).createCreditCurve(captor.capture());
        SaveCreditCurveCommand command = captor.getValue();
        assertThat(command.active()).isFalse();
        assertThat(command.source().name()).isEqualTo("MARKET_DATA");
        assertThat(command.sourceRatingBucket()).isEqualTo("A");
        assertThat(command.sourceSeriesId()).isEqualTo("BAMLC0A3CA");
        assertThat(command.sourceSpread()).isEqualTo(0.0067);
        assertThat(command.points()).hasSize(7);
    }

    @Test
    void rejectsStaleCurveUnlessExplicitlyAllowed() {
        when(gateway.getCreditCurve("A+", "USD", valuationDate, 0.40)).thenReturn(marketCurve(true));

        assertThatThrownBy(() -> service.importMarketCreditCurve(counterpartyId, valuationDate, 0.40, null, false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("stale");
        verify(store, never()).createCreditCurve(any());
    }

    @Test
    void rejectsCounterpartyWithoutSupportedRatingBeforeCallingBlemberg() {
        when(xvaStore.findCounterparty(counterpartyId)).thenReturn(Optional.of(
                new Counterparty(counterpartyId, "Unrated", null, null, true, Instant.now(), Instant.now())));

        assertThatThrownBy(() -> service.importMarketCreditCurve(counterpartyId, valuationDate, 0.40, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Counterparty creditRating is required");
        verify(gateway, never()).getCreditCurve(any(), any(), any(), anyDouble());
    }

    private MarketCreditCurve marketCurve(boolean stale) {
        List<MarketCreditCurve.Point> points = List.of(6, 12, 24, 36, 60, 84, 120).stream()
                .map(months -> new MarketCreditCurve.Point(valuationDate.plusMonths(months), months / 1200.0))
                .toList();
        return new MarketCreditCurve(
                "USD A Rating OAS Credit Proxy", "CUMULATIVE_DEFAULT_PROBABILITY", "A+", "A", "USD",
                valuationDate, 0.40, 0.0067, "DECIMAL", 0.0111666667,
                LocalDate.parse("2026-07-30"), Instant.parse("2026-08-02T04:27:07Z"),
                "FRED_ICE_BOFA_RATING_OAS", "BAMLC0A3CA", "RATING_OAS_FLAT_HAZARD_PROXY",
                true, stale, points);
    }
}

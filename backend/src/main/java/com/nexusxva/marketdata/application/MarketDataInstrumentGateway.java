package com.nexusxva.marketdata.application;

import com.nexusxva.marketdata.domain.MarketInstrument;

import java.util.Optional;

public interface MarketDataInstrumentGateway {

    Optional<MarketInstrument> findInstrument(String symbol);
}

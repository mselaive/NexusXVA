package com.nexusxva.marketdata.application;

import com.nexusxva.marketdata.domain.HistoricalPriceSeries;
import java.util.Collection;
import java.util.List;

public interface HistoricalMarketDataGateway {
    List<HistoricalPriceSeries> dailyCloses(Collection<String> symbols, int observations);
}

package com.nexusxva.marketdata.application;

import com.nexusxva.marketdata.domain.MarketDiscountCurve;
import java.time.LocalDate;

public interface MarketDataCurveGateway {
    MarketDiscountCurve getDiscountCurve(String currency, LocalDate valuationDate);
}

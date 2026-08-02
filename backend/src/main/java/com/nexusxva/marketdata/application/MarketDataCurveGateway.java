package com.nexusxva.marketdata.application;

import com.nexusxva.marketdata.domain.MarketDiscountCurve;
import com.nexusxva.marketdata.domain.MarketCreditCurve;
import java.time.LocalDate;

public interface MarketDataCurveGateway {
    MarketDiscountCurve getDiscountCurve(String currency, LocalDate valuationDate);
    MarketCreditCurve getCreditCurve(String creditRating, String currency, LocalDate valuationDate, double recoveryRate);
}

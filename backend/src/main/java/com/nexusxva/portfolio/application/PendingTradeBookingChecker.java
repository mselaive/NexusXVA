package com.nexusxva.portfolio.application;

import java.util.UUID;

public interface PendingTradeBookingChecker {

    boolean hasPendingBookings(UUID portfolioId);
}


package com.nexusxva.tradebooking.application;

import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TradeBookingStore {

    TradeBookingRequest create(
            UUID portfolioId,
            String portfolioName,
            CreateEuropeanOptionBookingCommand command,
            BookingActor submittedBy
    );

    Optional<TradeBookingRequest> findById(UUID bookingId);

    TradeBookingRequest findByIdForUpdate(UUID bookingId);

    List<TradeBookingRequest> findSubmittedBy(UUID userId);

    List<TradeBookingRequest> findAllSubmitted();

    Page<TradeBookingRequest> search(
            TradeBookingStatus status,
            UUID portfolioId,
            String symbol,
            Pageable pageable
    );

    TradeBookingRequest confirm(UUID bookingId, BookingActor reviewer, UUID confirmedPositionId);

    TradeBookingRequest reject(UUID bookingId, BookingActor reviewer, String reason);

    boolean existsPendingForPortfolio(UUID portfolioId);
}


package com.nexusxva.tradelifecycle.application;

import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TradeLifecycleStore {

    TradeLifecycleRequest createAmend(
            EuropeanOptionPosition originalPosition,
            String portfolioName,
            AddEuropeanOptionPositionCommand requestedTerms,
            BookingActor submittedBy
    );

    TradeLifecycleRequest createCancel(
            EuropeanOptionPosition originalPosition,
            String portfolioName,
            BookingActor submittedBy
    );

    Optional<TradeLifecycleRequest> findById(UUID requestId);

    TradeLifecycleRequest findByIdForUpdate(UUID requestId);

    List<TradeLifecycleRequest> findSubmittedBy(UUID userId);

    List<TradeLifecycleRequest> findAllSubmitted();

    Page<TradeLifecycleRequest> search(TradeLifecycleRequestStatus status, UUID portfolioId, String symbol, Pageable pageable);

    boolean existsPendingForPosition(UUID positionId);

    TradeLifecycleRequest approve(UUID requestId, BookingActor reviewer, UUID resultingPositionId);

    TradeLifecycleRequest reject(UUID requestId, BookingActor reviewer, String reason);
}

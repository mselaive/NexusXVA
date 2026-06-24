package com.nexusxva.tradebooking.infrastructure;

import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.tradebooking.application.CreateEuropeanOptionBookingCommand;
import com.nexusxva.tradebooking.application.TradeBookingStore;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class JpaTradeBookingStore implements TradeBookingStore {

    private final TradeBookingJpaRepository repository;

    JpaTradeBookingStore(TradeBookingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TradeBookingRequest create(
            UUID portfolioId,
            String portfolioName,
            CreateEuropeanOptionBookingCommand command,
            BookingActor submittedBy
    ) {
        return repository.save(
                TradeBookingEntity.create(portfolioId, portfolioName, command, submittedBy)
        ).toDomain();
    }

    @Override
    public TradeBookingRequest createStrategy(
            UUID portfolioId,
            String portfolioName,
            com.nexusxva.tradebooking.application.CreateOptionStrategyBookingCommand command,
            UUID strategyId,
            BookingActor submittedBy
    ) {
        return repository.save(
                TradeBookingEntity.createStrategy(portfolioId, portfolioName, command, strategyId, submittedBy)
        ).toDomain();
    }

    @Override
    public Optional<TradeBookingRequest> findById(UUID bookingId) {
        return repository.findById(bookingId).map(TradeBookingEntity::toDomain);
    }

    @Override
    public TradeBookingRequest findByIdForUpdate(UUID bookingId) {
        return findEntityForUpdate(bookingId).toDomain();
    }

    @Override
    public List<TradeBookingRequest> findSubmittedBy(UUID userId) {
        return repository.findBySubmittedByUserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(TradeBookingEntity::toDomain)
                .toList();
    }

    @Override
    public List<TradeBookingRequest> findAllSubmitted() {
        return repository.findAllByOrderBySubmittedAtDesc()
                .stream()
                .map(TradeBookingEntity::toDomain)
                .toList();
    }

    @Override
    public Page<TradeBookingRequest> search(
            TradeBookingStatus status,
            UUID portfolioId,
            String symbol,
            Pageable pageable
    ) {
        Specification<TradeBookingEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (portfolioId != null) {
                predicates.add(builder.equal(root.get("portfolioId"), portfolioId));
            }
            if (symbol != null) {
                predicates.add(builder.equal(root.get("underlyingSymbol"), symbol));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return repository.findAll(specification, pageable).map(TradeBookingEntity::toDomain);
    }

    @Override
    public TradeBookingRequest confirm(UUID bookingId, BookingActor reviewer, List<UUID> confirmedPositionIds) {
        TradeBookingEntity entity = findEntityForUpdate(bookingId);
        entity.confirm(reviewer, confirmedPositionIds);
        return repository.save(entity).toDomain();
    }

    @Override
    public TradeBookingRequest reject(UUID bookingId, BookingActor reviewer, String reason) {
        TradeBookingEntity entity = findEntityForUpdate(bookingId);
        entity.reject(reviewer, reason);
        return repository.save(entity).toDomain();
    }

    @Override
    public boolean existsPendingForPortfolio(UUID portfolioId) {
        return repository.existsByPortfolioIdAndStatus(portfolioId, TradeBookingStatus.PENDING_VALIDATION);
    }

    private TradeBookingEntity findEntityForUpdate(UUID bookingId) {
        return repository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade booking not found"));
    }
}

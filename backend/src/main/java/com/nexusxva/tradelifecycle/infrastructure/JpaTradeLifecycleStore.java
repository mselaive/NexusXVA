package com.nexusxva.tradelifecycle.infrastructure;

import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradelifecycle.application.TradeLifecycleStore;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
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
class JpaTradeLifecycleStore implements TradeLifecycleStore {

    private final TradeLifecycleJpaRepository repository;

    JpaTradeLifecycleStore(TradeLifecycleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TradeLifecycleRequest createAmend(
            EuropeanOptionPosition originalPosition,
            String portfolioName,
            AddEuropeanOptionPositionCommand requestedTerms,
            BookingActor submittedBy
    ) {
        return repository.save(
                TradeLifecycleEntity.amend(originalPosition, portfolioName, requestedTerms, submittedBy)
        ).toDomain();
    }

    @Override
    public TradeLifecycleRequest createCancel(
            EuropeanOptionPosition originalPosition,
            String portfolioName,
            BookingActor submittedBy
    ) {
        return repository.save(
                TradeLifecycleEntity.cancel(originalPosition, portfolioName, submittedBy)
        ).toDomain();
    }

    @Override
    public Optional<TradeLifecycleRequest> findById(UUID requestId) {
        return repository.findById(requestId).map(TradeLifecycleEntity::toDomain);
    }

    @Override
    public TradeLifecycleRequest findByIdForUpdate(UUID requestId) {
        return findEntityForUpdate(requestId).toDomain();
    }

    @Override
    public List<TradeLifecycleRequest> findSubmittedBy(UUID userId) {
        return repository.findBySubmittedByUserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(TradeLifecycleEntity::toDomain)
                .toList();
    }

    @Override
    public List<TradeLifecycleRequest> findAllSubmitted() {
        return repository.findAllByOrderBySubmittedAtDesc()
                .stream()
                .map(TradeLifecycleEntity::toDomain)
                .toList();
    }

    @Override
    public Page<TradeLifecycleRequest> search(
            TradeLifecycleRequestStatus status,
            UUID portfolioId,
            String symbol,
            Pageable pageable
    ) {
        Specification<TradeLifecycleEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (portfolioId != null) {
                predicates.add(builder.equal(root.get("portfolioId"), portfolioId));
            }
            if (symbol != null) {
                predicates.add(builder.or(
                        builder.equal(root.get("originalUnderlyingSymbol"), symbol),
                        builder.equal(root.get("requestedUnderlyingSymbol"), symbol)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return repository.findAll(specification, pageable).map(TradeLifecycleEntity::toDomain);
    }

    @Override
    public boolean existsPendingForPosition(UUID positionId) {
        return repository.existsByPositionIdAndStatus(positionId, TradeLifecycleRequestStatus.PENDING_VALIDATION);
    }

    @Override
    public TradeLifecycleRequest approve(UUID requestId, BookingActor reviewer, UUID resultingPositionId) {
        TradeLifecycleEntity entity = findEntityForUpdate(requestId);
        entity.approve(reviewer, resultingPositionId);
        return repository.save(entity).toDomain();
    }

    @Override
    public TradeLifecycleRequest reject(UUID requestId, BookingActor reviewer, String reason) {
        TradeLifecycleEntity entity = findEntityForUpdate(requestId);
        entity.reject(reviewer, reason);
        return repository.save(entity).toDomain();
    }

    private TradeLifecycleEntity findEntityForUpdate(UUID requestId) {
        return repository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Lifecycle request not found"));
    }
}

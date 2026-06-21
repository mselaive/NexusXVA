package com.nexusxva.tradelifecycle.infrastructure;

import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TradeLifecycleJpaRepository
        extends JpaRepository<TradeLifecycleEntity, UUID>, JpaSpecificationExecutor<TradeLifecycleEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from TradeLifecycleEntity request where request.id = :requestId")
    Optional<TradeLifecycleEntity> findByIdForUpdate(@Param("requestId") UUID requestId);

    List<TradeLifecycleEntity> findBySubmittedByUserIdOrderBySubmittedAtDesc(UUID userId);

    List<TradeLifecycleEntity> findAllByOrderBySubmittedAtDesc();

    boolean existsByPositionIdAndStatus(UUID positionId, TradeLifecycleRequestStatus status);
}

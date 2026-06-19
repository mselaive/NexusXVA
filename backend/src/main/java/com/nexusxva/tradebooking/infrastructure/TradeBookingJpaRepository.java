package com.nexusxva.tradebooking.infrastructure;

import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TradeBookingJpaRepository
        extends JpaRepository<TradeBookingEntity, UUID>, JpaSpecificationExecutor<TradeBookingEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select booking from TradeBookingEntity booking where booking.id = :bookingId")
    Optional<TradeBookingEntity> findByIdForUpdate(@Param("bookingId") UUID bookingId);

    List<TradeBookingEntity> findBySubmittedByUserIdOrderBySubmittedAtDesc(UUID userId);

    List<TradeBookingEntity> findAllByOrderBySubmittedAtDesc();

    boolean existsByPortfolioIdAndStatus(UUID portfolioId, TradeBookingStatus status);
}


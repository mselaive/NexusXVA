package com.nexusxva.portfolio.infrastructure;

import com.nexusxva.portfolio.domain.PositionLifecycleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CashEquityPositionJpaRepository extends JpaRepository<CashEquityPositionEntity, UUID> {

    @Query("select position from CashEquityPositionEntity position where position.portfolio.id = :portfolioId order by position.createdAt")
    List<CashEquityPositionEntity> findByPortfolioId(@Param("portfolioId") UUID portfolioId);

    @Query("""
            select position
            from CashEquityPositionEntity position
            where position.portfolio.id = :portfolioId
              and position.lifecycleStatus = :status
            order by position.createdAt
            """)
    List<CashEquityPositionEntity> findByPortfolioIdAndLifecycleStatus(
            @Param("portfolioId") UUID portfolioId,
            @Param("status") PositionLifecycleStatus status
    );

    @Query("""
            select position
            from CashEquityPositionEntity position
            where position.portfolio.id = :portfolioId
              and position.id = :positionId
            """)
    Optional<CashEquityPositionEntity> findByPortfolioIdAndPositionId(
            @Param("portfolioId") UUID portfolioId,
            @Param("positionId") UUID positionId
    );
}

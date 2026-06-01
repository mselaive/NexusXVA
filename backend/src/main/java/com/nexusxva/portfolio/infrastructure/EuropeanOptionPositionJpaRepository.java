package com.nexusxva.portfolio.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface EuropeanOptionPositionJpaRepository extends JpaRepository<EuropeanOptionPositionEntity, UUID> {

    @Query("""
            select position
            from EuropeanOptionPositionEntity position
            where position.portfolio.id = :portfolioId
            order by position.createdAt asc
            """)
    List<EuropeanOptionPositionEntity> findByPortfolioId(@Param("portfolioId") UUID portfolioId);

    @Query("""
            select position
            from EuropeanOptionPositionEntity position
            where position.portfolio.id = :portfolioId
              and position.id = :positionId
            """)
    Optional<EuropeanOptionPositionEntity> findByPortfolioIdAndPositionId(
            @Param("portfolioId") UUID portfolioId,
            @Param("positionId") UUID positionId
    );
}

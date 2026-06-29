package com.nexusxva.portfolio.infrastructure;

import com.nexusxva.portfolio.domain.PortfolioSummary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PortfolioJpaRepository extends JpaRepository<PortfolioEntity, UUID> {

    @Query("""
            select new com.nexusxva.portfolio.domain.PortfolioSummary(
                portfolio.id,
                portfolio.name,
                portfolio.description,
                portfolio.baseCurrency,
                portfolio.createdAt,
                portfolio.updatedAt,
                portfolio.archivedAt,
                count(position.id)
            )
            from PortfolioEntity portfolio
            left join portfolio.positions position
            where portfolio.archivedAt is null
            group by portfolio.id, portfolio.name, portfolio.description, portfolio.baseCurrency, portfolio.createdAt, portfolio.updatedAt, portfolio.archivedAt
            order by portfolio.createdAt asc
            """)
    List<PortfolioSummary> findSummaries();

    @Query("""
            select portfolio
            from PortfolioEntity portfolio
            where portfolio.id = :portfolioId
              and portfolio.archivedAt is null
            """)
    Optional<PortfolioEntity> findActiveById(UUID portfolioId);

    boolean existsByIdAndArchivedAtIsNull(UUID portfolioId);
}

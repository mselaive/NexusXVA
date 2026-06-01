package com.nexusxva.portfolio.infrastructure;

import com.nexusxva.portfolio.domain.PortfolioSummary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
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
                count(position.id)
            )
            from PortfolioEntity portfolio
            left join portfolio.positions position
            group by portfolio.id, portfolio.name, portfolio.description, portfolio.baseCurrency, portfolio.createdAt, portfolio.updatedAt
            order by portfolio.createdAt asc
            """)
    List<PortfolioSummary> findSummaries();
}

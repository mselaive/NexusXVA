package com.nexusxva.portfolio.infrastructure;

import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import com.nexusxva.portfolio.application.AddCashEquityPositionCommand;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.portfolio.application.CreatePortfolioCommand;
import com.nexusxva.portfolio.application.UpdatePortfolioCommand;
import com.nexusxva.portfolio.domain.CashEquityPosition;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.portfolio.domain.PositionLifecycleStatus;
import com.nexusxva.shared.error.ResourceNotFoundException;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaPortfolioStore implements PortfolioStore {

    private final PortfolioJpaRepository portfolioJpaRepository;
    private final EuropeanOptionPositionJpaRepository positionJpaRepository;
    private final CashEquityPositionJpaRepository cashEquityPositionJpaRepository;

    JpaPortfolioStore(
            PortfolioJpaRepository portfolioJpaRepository,
            EuropeanOptionPositionJpaRepository positionJpaRepository,
            CashEquityPositionJpaRepository cashEquityPositionJpaRepository
    ) {
        this.portfolioJpaRepository = portfolioJpaRepository;
        this.positionJpaRepository = positionJpaRepository;
        this.cashEquityPositionJpaRepository = cashEquityPositionJpaRepository;
    }

    @Override
    public Portfolio createPortfolio(CreatePortfolioCommand command) {
        PortfolioEntity portfolio = PortfolioEntity.create(command.name(), command.description(), command.baseCurrency());
        return portfolioJpaRepository.save(portfolio).toDomain();
    }

    @Override
    public List<PortfolioSummary> listPortfolioSummaries() {
        return portfolioJpaRepository.findSummaries();
    }

    @Override
    public Optional<Portfolio> findPortfolio(UUID portfolioId) {
        return portfolioJpaRepository.findActiveById(portfolioId)
                .map(PortfolioEntity::toDomain);
    }

    @Override
    public boolean existsPortfolio(UUID portfolioId) {
        return portfolioJpaRepository.existsByIdAndArchivedAtIsNull(portfolioId);
    }

    @Override
    public Portfolio updatePortfolio(UUID portfolioId, UpdatePortfolioCommand command) {
        PortfolioEntity portfolio = findPortfolioEntity(portfolioId);
        portfolio.update(command.name(), command.description(), command.baseCurrency());
        return portfolio.toDomain();
    }

    @Override
    public void archivePortfolio(UUID portfolioId, UUID archivedByUserId, String reason) {
        PortfolioEntity portfolio = findPortfolioEntity(portfolioId);
        portfolio.archive(archivedByUserId, reason);
        portfolioJpaRepository.save(portfolio);
    }

    @Override
    public EuropeanOptionPosition addEuropeanOptionPosition(
            UUID portfolioId,
            AddEuropeanOptionPositionCommand command
    ) {
        PortfolioEntity portfolio = findPortfolioEntity(portfolioId);

        EuropeanOptionPositionEntity position = EuropeanOptionPositionEntity.create(
                portfolio,
                command.underlyingSymbol(),
                command.optionType(),
                command.strike(),
                command.maturityDate(),
                command.quantity(),
                command.executionPrice(),
                command.strategyId(),
                command.strategyType(),
                command.strategyName(),
                command.strategyLegIndex()
        );

        return positionJpaRepository.save(position).toDomain();
    }

    @Override
    public CashEquityPosition addCashEquityPosition(
            UUID portfolioId,
            AddCashEquityPositionCommand command
    ) {
        PortfolioEntity portfolio = findPortfolioEntity(portfolioId);
        CashEquityPositionEntity position = CashEquityPositionEntity.create(
                portfolio,
                command.underlyingSymbol(),
                command.quantity(),
                command.executionPrice()
        );
        return cashEquityPositionJpaRepository.save(position).toDomain();
    }

    @Override
    public List<EuropeanOptionPosition> findEuropeanOptionPositions(UUID portfolioId) {
        return positionJpaRepository.findByPortfolioId(portfolioId)
                .stream()
                .map(EuropeanOptionPositionEntity::toDomain)
                .toList();
    }

    @Override
    public List<EuropeanOptionPosition> findActiveEuropeanOptionPositions(UUID portfolioId) {
        return positionJpaRepository.findActiveByPortfolioId(portfolioId)
                .stream()
                .map(EuropeanOptionPositionEntity::toDomain)
                .toList();
    }

    @Override
    public List<CashEquityPosition> findCashEquityPositions(UUID portfolioId) {
        return cashEquityPositionJpaRepository.findByPortfolioId(portfolioId)
                .stream()
                .map(CashEquityPositionEntity::toDomain)
                .toList();
    }

    @Override
    public List<CashEquityPosition> findActiveCashEquityPositions(UUID portfolioId) {
        return cashEquityPositionJpaRepository.findByPortfolioIdAndLifecycleStatus(portfolioId, PositionLifecycleStatus.ACTIVE)
                .stream()
                .map(CashEquityPositionEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<EuropeanOptionPosition> findEuropeanOptionPosition(UUID portfolioId, UUID positionId) {
        return positionJpaRepository.findByPortfolioIdAndPositionId(portfolioId, positionId)
                .map(EuropeanOptionPositionEntity::toDomain);
    }

    @Override
    public Optional<EuropeanOptionPosition> findEuropeanOptionPosition(UUID positionId) {
        return positionJpaRepository.findById(positionId)
                .map(EuropeanOptionPositionEntity::toDomain);
    }

    @Override
    public void markPositionCancelled(UUID positionId) {
        EuropeanOptionPositionEntity position = findPositionEntity(positionId);
        position.markCancelled();
        positionJpaRepository.save(position);
    }

    @Override
    public void markPositionAmended(UUID positionId) {
        EuropeanOptionPositionEntity position = findPositionEntity(positionId);
        position.markAmended();
        positionJpaRepository.save(position);
    }

    private PortfolioEntity findPortfolioEntity(UUID portfolioId) {
        return portfolioJpaRepository.findActiveById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    }

    private EuropeanOptionPositionEntity findPositionEntity(UUID positionId) {
        return positionJpaRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));
    }

}

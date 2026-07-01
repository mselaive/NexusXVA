package com.nexusxva.xva.infrastructure;

import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.xva.application.CreateCounterpartyCommand;
import com.nexusxva.xva.application.CreateNettingSetCommand;
import com.nexusxva.xva.application.UpdateNettingSetCollateralCommand;
import com.nexusxva.xva.application.XvaStore;
import com.nexusxva.xva.domain.Counterparty;
import com.nexusxva.xva.domain.NettingSet;
import com.nexusxva.xva.domain.NettingSetPortfolio;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
class JdbcXvaStore implements XvaStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcXvaStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Counterparty createCounterparty(CreateCounterpartyCommand command) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        try {
            jdbcTemplate.update("""
                    INSERT INTO counterparties (id, name, external_id, credit_rating, active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, TRUE, ?, ?)
                    """, id, command.name(), command.externalId(), command.creditRating(), now, now);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Counterparty already exists");
        }
        return findCounterparty(id).orElseThrow();
    }

    @Override
    public List<Counterparty> listCounterparties() {
        return jdbcTemplate.query("""
                SELECT id, name, external_id, credit_rating, active, created_at, updated_at
                FROM counterparties
                ORDER BY name
                """, this::mapCounterparty);
    }

    @Override
    public Optional<Counterparty> findCounterparty(UUID counterpartyId) {
        return jdbcTemplate.query("""
                SELECT id, name, external_id, credit_rating, active, created_at, updated_at
                FROM counterparties
                WHERE id = ?
                """, this::mapCounterparty, counterpartyId).stream().findFirst();
    }

    @Override
    public NettingSet createNettingSet(CreateNettingSetCommand command) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        try {
            jdbcTemplate.update("""
                    INSERT INTO netting_sets
                        (id, counterparty_id, name, base_currency, collateral_amount, collateral_currency, active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, TRUE, ?, ?)
                    """,
                    id,
                    command.counterpartyId(),
                    command.name(),
                    command.baseCurrency(),
                    command.collateralAmount(),
                    command.collateralCurrency(),
                    now,
                    now);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Netting set already exists for this counterparty");
        }
        return findNettingSet(id).orElseThrow();
    }

    @Override
    public List<NettingSet> listNettingSets() {
        return jdbcTemplate.query(nettingSetSql(""), nettingSetMapper());
    }

    @Override
    public Optional<NettingSet> findNettingSet(UUID nettingSetId) {
        return jdbcTemplate.query(nettingSetSql("WHERE ns.id = ?"), nettingSetMapper(), nettingSetId)
                .stream()
                .findFirst();
    }

    @Override
    public NettingSet assignPortfolio(UUID nettingSetId, UUID portfolioId) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO netting_set_portfolios (netting_set_id, portfolio_id, assigned_at)
                    VALUES (?, ?, ?)
                    """, nettingSetId, portfolioId, Instant.now());
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Portfolio is already assigned to a netting set");
        }
        return findNettingSet(nettingSetId).orElseThrow();
    }

    @Override
    public NettingSet removePortfolio(UUID nettingSetId, UUID portfolioId) {
        jdbcTemplate.update("""
                DELETE FROM netting_set_portfolios
                WHERE netting_set_id = ? AND portfolio_id = ?
                """, nettingSetId, portfolioId);
        return findNettingSet(nettingSetId).orElseThrow();
    }

    @Override
    public NettingSet updateCollateral(UUID nettingSetId, UpdateNettingSetCollateralCommand command) {
        jdbcTemplate.update("""
                UPDATE netting_sets
                SET collateral_amount = ?, updated_at = ?
                WHERE id = ?
                """, command.collateralAmount(), Instant.now(), nettingSetId);
        return findNettingSet(nettingSetId).orElseThrow();
    }

    private String nettingSetSql(String whereClause) {
        return """
                SELECT ns.id,
                       ns.counterparty_id,
                       cp.name AS counterparty_name,
                       ns.name,
                       ns.base_currency,
                       ns.collateral_amount,
                       ns.collateral_currency,
                       ns.active,
                       ns.created_at,
                       ns.updated_at
                FROM netting_sets ns
                JOIN counterparties cp ON cp.id = ns.counterparty_id
                %s
                ORDER BY cp.name, ns.name
                """.formatted(whereClause);
    }

    private RowMapper<NettingSet> nettingSetMapper() {
        return (rs, rowNum) -> {
            UUID id = rs.getObject("id", UUID.class);
            return new NettingSet(
                    id,
                    rs.getObject("counterparty_id", UUID.class),
                    rs.getString("counterparty_name"),
                    rs.getString("name"),
                    rs.getString("base_currency"),
                    rs.getBigDecimal("collateral_amount"),
                    rs.getString("collateral_currency"),
                    rs.getBoolean("active"),
                    rs.getObject("created_at", Instant.class),
                    rs.getObject("updated_at", Instant.class),
                    portfolios(id)
            );
        };
    }

    private List<NettingSetPortfolio> portfolios(UUID nettingSetId) {
        return jdbcTemplate.query("""
                SELECT p.id AS portfolio_id,
                       p.name AS portfolio_name,
                       p.base_currency,
                       nsp.assigned_at
                FROM netting_set_portfolios nsp
                JOIN portfolios p ON p.id = nsp.portfolio_id
                WHERE nsp.netting_set_id = ?
                  AND p.archived_at IS NULL
                ORDER BY p.name
                """, this::mapPortfolio, nettingSetId);
    }

    private Counterparty mapCounterparty(ResultSet rs, int rowNum) throws SQLException {
        return new Counterparty(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("external_id"),
                rs.getString("credit_rating"),
                rs.getBoolean("active"),
                rs.getObject("created_at", Instant.class),
                rs.getObject("updated_at", Instant.class)
        );
    }

    private NettingSetPortfolio mapPortfolio(ResultSet rs, int rowNum) throws SQLException {
        return new NettingSetPortfolio(
                rs.getObject("portfolio_id", UUID.class),
                rs.getString("portfolio_name"),
                rs.getString("base_currency"),
                rs.getObject("assigned_at", Instant.class)
        );
    }
}

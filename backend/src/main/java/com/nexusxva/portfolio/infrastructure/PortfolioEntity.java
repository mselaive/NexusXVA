package com.nexusxva.portfolio.infrastructure;

import com.nexusxva.portfolio.domain.Portfolio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "portfolios")
class PortfolioEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by_user_id")
    private UUID archivedByUserId;

    @Column(name = "archive_reason", length = 500)
    private String archiveReason;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<EuropeanOptionPositionEntity> positions = new ArrayList<>();

    protected PortfolioEntity() {
    }

    private PortfolioEntity(UUID id, String name, String description, String baseCurrency, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.baseCurrency = baseCurrency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static PortfolioEntity create(String name, String description, String baseCurrency) {
        Instant now = Instant.now();
        Portfolio portfolio = new Portfolio(UUID.randomUUID(), name, description, baseCurrency, now, now, null, null, null, List.of());
        return new PortfolioEntity(
                portfolio.id(),
                portfolio.name(),
                portfolio.description(),
                portfolio.baseCurrency(),
                portfolio.createdAt(),
                portfolio.updatedAt()
        );
    }

    UUID getId() {
        return id;
    }

    void update(String name, String description, String baseCurrency) {
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description.isBlank() ? null : description.trim();
        }
        if (baseCurrency != null) {
            this.baseCurrency = baseCurrency.trim().toUpperCase(Locale.ROOT);
        }
        this.updatedAt = Instant.now();
    }

    void archive(UUID archivedByUserId, String reason) {
        this.archivedAt = Instant.now();
        this.archivedByUserId = archivedByUserId;
        this.archiveReason = reason == null || reason.isBlank() ? null : reason.trim();
        this.updatedAt = this.archivedAt;
    }

    Portfolio toDomain() {
        return new Portfolio(
                id,
                name,
                description,
                baseCurrency,
                createdAt,
                updatedAt,
                archivedAt,
                archivedByUserId,
                archiveReason,
                positions.stream()
                        .map(EuropeanOptionPositionEntity::toDomain)
                        .toList()
        );
    }
}

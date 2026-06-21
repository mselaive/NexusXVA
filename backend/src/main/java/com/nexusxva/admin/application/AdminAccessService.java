package com.nexusxva.admin.application;

import com.nexusxva.admin.api.AdminFeaturePermissionResponse;
import com.nexusxva.admin.api.AdminPortfolioAccessResponse;
import com.nexusxva.admin.api.AdminPortfolioSummaryResponse;
import com.nexusxva.admin.api.AdminUserAccessResponse;
import com.nexusxva.admin.api.AdminUserPageResponse;
import com.nexusxva.admin.api.AdminWorkflowBookingResponse;
import com.nexusxva.admin.api.AdminWorkflowLinkResponse;
import com.nexusxva.admin.api.AdminWorkflowMapResponse;
import com.nexusxva.admin.api.AdminWorkflowNodeResponse;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.shared.error.ResourceNotFoundException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccessService {

    private static final List<NodeDefinition> NODES = List.of(
            new NodeDefinition("SUBMITTED", "Booked", "All trade requests submitted by Front Office."),
            new NodeDefinition("PENDING_VALIDATION", "Waiting BO", "Bookings waiting for Back Office review."),
            new NodeDefinition("CONFIRMED", "Accepted", "Bookings approved by BO and converted into confirmed positions."),
            new NodeDefinition("REJECTED", "Rejected", "Bookings rejected by BO with a reason."),
            new NodeDefinition("LIFECYCLE_REQUESTED", "Lifecycle requested", "Amendment and cancellation requests submitted by Front Office."),
            new NodeDefinition("LIFECYCLE_WAITING_BO", "Lifecycle waiting BO", "Lifecycle requests waiting for Back Office review."),
            new NodeDefinition("LIFECYCLE_APPROVED", "Lifecycle approved", "Lifecycle requests approved by BO."),
            new NodeDefinition("LIFECYCLE_REJECTED", "Lifecycle rejected", "Lifecycle requests rejected by BO.")
    );

    private final JdbcTemplate jdbcTemplate;

    public AdminAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public AdminUserPageResponse users(String query, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String pattern = query == null || query.isBlank() ? "%" : "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        Long total = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM auth_user_accounts
                WHERE lower(username) LIKE ? OR lower(display_name) LIKE ?
                """,
                Long.class,
                pattern,
                pattern
        );
        List<UUID> ids = jdbcTemplate.query(
                """
                SELECT id
                FROM auth_user_accounts
                WHERE lower(username) LIKE ? OR lower(display_name) LIKE ?
                ORDER BY display_name, username
                LIMIT ? OFFSET ?
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                pattern,
                pattern,
                safeSize,
                safePage * safeSize
        );
        long totalElements = total == null ? 0 : total;
        return new AdminUserPageResponse(
                ids.stream().map(this::user).toList(),
                safePage,
                safeSize,
                totalElements,
                (int) Math.ceil(totalElements / (double) safeSize)
        );
    }

    @Transactional(readOnly = true)
    public AdminUserAccessResponse user(UUID userId) {
        UserRow user = findUser(userId);
        return new AdminUserAccessResponse(
                user.id(),
                user.username(),
                user.displayName(),
                user.active(),
                user.createdAt(),
                user.updatedAt(),
                user.lastLoginAt(),
                groups(userId),
                permissions(userId),
                portfolioAccess(userId, user.portfolioAccessMode())
        );
    }

    @Transactional(readOnly = true)
    public List<AdminPortfolioSummaryResponse> portfolios() {
        return allPortfolios();
    }

    @Transactional
    public AdminUserAccessResponse updateGroups(UUID userId, List<String> groups, AuthSession updatedBy) {
        ensureUserExists(userId);
        List<String> normalized = groups.stream()
                .map(group -> group.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        ensureGroupsExist(normalized);
        ensureAdminGroupIsNotRemovedUnsafely(userId, normalized, updatedBy);
        jdbcTemplate.update("DELETE FROM auth_user_group_memberships WHERE user_id = ?", userId);
        for (String group : normalized) {
            jdbcTemplate.update(
                    """
                    INSERT INTO auth_user_group_memberships (user_id, group_id)
                    SELECT ?, id FROM auth_groups WHERE code = ?
                    """,
                    userId,
                    group
            );
        }
        jdbcTemplate.update("UPDATE auth_user_accounts SET updated_at = ? WHERE id = ?", Timestamp.from(Instant.now()), userId);
        return user(userId);
    }

    private void ensureAdminGroupIsNotRemovedUnsafely(UUID userId, List<String> requestedGroups, AuthSession updatedBy) {
        if (!groups(userId).contains("ADMIN") || requestedGroups.contains("ADMIN")) {
            return;
        }
        if (updatedBy != null && updatedBy.user().id().equals(userId)) {
            throw new ConflictException("You cannot remove your own ADMIN group");
        }
        Long remainingAdmins = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM auth_user_accounts u
                JOIN auth_user_group_memberships m ON m.user_id = u.id
                JOIN auth_groups g ON g.id = m.group_id
                WHERE g.code = 'ADMIN'
                  AND u.active = TRUE
                  AND u.id <> ?
                """,
                Long.class,
                userId
        );
        if (remainingAdmins == null || remainingAdmins == 0) {
            throw new ConflictException("At least one ADMIN user is required");
        }
    }

    @Transactional
    public AdminUserAccessResponse updatePermissions(
            UUID userId,
            Map<String, Boolean> permissions,
            AuthSession updatedBy
    ) {
        ensureUserExists(userId);
        Set<String> validCodes = permissionCatalog().stream()
                .map(PermissionRow::code)
                .collect(Collectors.toSet());
        Instant now = Instant.now();
        permissions.forEach((code, enabled) -> {
            String normalized = code.trim().toUpperCase(Locale.ROOT);
            if (!validCodes.contains(normalized)) {
                throw new ResourceNotFoundException("Feature permission not found");
            }
            jdbcTemplate.update(
                    """
                    INSERT INTO auth_user_feature_permission_overrides (
                        user_id, permission_code, enabled, updated_at, updated_by_user_id,
                        updated_by_username, updated_by_display_name
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (user_id, permission_code)
                    DO UPDATE SET enabled = EXCLUDED.enabled,
                                  updated_at = EXCLUDED.updated_at,
                                  updated_by_user_id = EXCLUDED.updated_by_user_id,
                                  updated_by_username = EXCLUDED.updated_by_username,
                                  updated_by_display_name = EXCLUDED.updated_by_display_name
                    """,
                    userId,
                    normalized,
                    Boolean.TRUE.equals(enabled),
                    Timestamp.from(now),
                    updatedBy == null ? null : updatedBy.user().id(),
                    updatedBy == null ? null : updatedBy.user().username(),
                    updatedBy == null ? null : updatedBy.user().displayName()
            );
        });
        return user(userId);
    }

    @Transactional
    public AdminUserAccessResponse updatePortfolioAccess(
            UUID userId,
            String accessMode,
            List<UUID> portfolioIds,
            AuthSession grantedBy
    ) {
        ensureUserExists(userId);
        String normalizedMode = accessMode.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "SELECTED").contains(normalizedMode)) {
            throw new IllegalArgumentException("accessMode must be ALL or SELECTED");
        }
        jdbcTemplate.update("UPDATE auth_user_accounts SET portfolio_access_mode = ?, updated_at = ? WHERE id = ?",
                normalizedMode,
                Timestamp.from(Instant.now()),
                userId);
        jdbcTemplate.update("DELETE FROM auth_user_portfolio_access WHERE user_id = ?", userId);
        if ("SELECTED".equals(normalizedMode)) {
            for (UUID portfolioId : portfolioIds.stream().distinct().toList()) {
                ensurePortfolioExists(portfolioId);
                jdbcTemplate.update(
                        """
                        INSERT INTO auth_user_portfolio_access (
                            user_id, portfolio_id, granted_at, granted_by_user_id,
                            granted_by_username, granted_by_display_name
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        userId,
                        portfolioId,
                        Timestamp.from(Instant.now()),
                        grantedBy == null ? null : grantedBy.user().id(),
                        grantedBy == null ? null : grantedBy.user().username(),
                        grantedBy == null ? null : grantedBy.user().displayName()
                );
            }
        }
        return user(userId);
    }

    @Transactional(readOnly = true)
    public AdminWorkflowMapResponse workflowMap(UUID portfolioId) {
        List<AdminWorkflowBookingResponse> bookings = workflowBookings(portfolioId);
        Map<String, List<AdminWorkflowBookingResponse>> byNode = new LinkedHashMap<>();
        byNode.put("SUBMITTED", bookings);
        byNode.put("PENDING_VALIDATION", filterByStatus(bookings, "PENDING_VALIDATION"));
        byNode.put("CONFIRMED", filterByStatus(bookings, "CONFIRMED"));
        byNode.put("REJECTED", filterByStatus(bookings, "REJECTED"));
        List<AdminWorkflowBookingResponse> lifecycleRequests = workflowLifecycleRequests(portfolioId);
        byNode.put("LIFECYCLE_REQUESTED", lifecycleRequests);
        byNode.put("LIFECYCLE_WAITING_BO", filterByStatus(lifecycleRequests, "PENDING_VALIDATION"));
        byNode.put("LIFECYCLE_APPROVED", filterByStatus(lifecycleRequests, "APPROVED"));
        byNode.put("LIFECYCLE_REJECTED", filterByStatus(lifecycleRequests, "REJECTED"));

        List<AdminWorkflowNodeResponse> nodes = NODES.stream()
                .map(node -> new AdminWorkflowNodeResponse(
                        node.id(),
                        node.label(),
                        node.description(),
                        byNode.get(node.id()).size(),
                        byNode.get(node.id())
                ))
                .toList();
        return new AdminWorkflowMapResponse(
                portfolioId,
                nodes,
                List.of(
                        new AdminWorkflowLinkResponse("SUBMITTED", "PENDING_VALIDATION", byNode.get("PENDING_VALIDATION").size()),
                        new AdminWorkflowLinkResponse("PENDING_VALIDATION", "CONFIRMED", byNode.get("CONFIRMED").size()),
                        new AdminWorkflowLinkResponse("PENDING_VALIDATION", "REJECTED", byNode.get("REJECTED").size()),
                        new AdminWorkflowLinkResponse("LIFECYCLE_REQUESTED", "LIFECYCLE_WAITING_BO", byNode.get("LIFECYCLE_WAITING_BO").size()),
                        new AdminWorkflowLinkResponse("LIFECYCLE_WAITING_BO", "LIFECYCLE_APPROVED", byNode.get("LIFECYCLE_APPROVED").size()),
                        new AdminWorkflowLinkResponse("LIFECYCLE_WAITING_BO", "LIFECYCLE_REJECTED", byNode.get("LIFECYCLE_REJECTED").size())
                )
        );
    }

    private AdminPortfolioAccessResponse portfolioAccess(UUID userId, String mode) {
        if ("ALL".equals(mode)) {
            return new AdminPortfolioAccessResponse(mode, allPortfolios());
        }
        String sql = """
                  SELECT p.id, p.name, p.base_currency, count(pos.id) AS position_count
                  FROM auth_user_portfolio_access access
                  JOIN portfolios p ON p.id = access.portfolio_id
                  LEFT JOIN portfolio_european_option_positions pos ON pos.portfolio_id = p.id
                  WHERE access.user_id = ?
                  GROUP BY p.id, p.name, p.base_currency, p.created_at
                  ORDER BY p.created_at ASC
                  """;
        return new AdminPortfolioAccessResponse(mode, jdbcTemplate.query(sql, this::portfolio, userId));
    }

    private List<AdminPortfolioSummaryResponse> allPortfolios() {
        return jdbcTemplate.query(
                """
                SELECT p.id, p.name, p.base_currency, count(pos.id) AS position_count
                FROM portfolios p
                LEFT JOIN portfolio_european_option_positions pos ON pos.portfolio_id = p.id
                GROUP BY p.id, p.name, p.base_currency, p.created_at
                ORDER BY p.created_at ASC
                """,
                this::portfolio
        );
    }

    private List<AdminFeaturePermissionResponse> permissions(UUID userId) {
        Map<String, Boolean> overrides = jdbcTemplate.query(
                """
                SELECT permission_code, enabled
                FROM auth_user_feature_permission_overrides
                WHERE user_id = ?
                """,
                rs -> {
                    Map<String, Boolean> result = new LinkedHashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString("permission_code"), rs.getBoolean("enabled"));
                    }
                    return result;
                },
                userId
        );
        return permissionCatalog().stream()
                .map(permission -> {
                    Boolean override = overrides.get(permission.code());
                    return new AdminFeaturePermissionResponse(
                            permission.code(),
                            permission.groupCode(),
                            permission.name(),
                            permission.description(),
                            override == null || override,
                            override
                    );
                })
                .toList();
    }

    private List<PermissionRow> permissionCatalog() {
        return jdbcTemplate.query(
                """
                SELECT code, group_code, name, description
                FROM auth_feature_permissions
                ORDER BY group_code, code
                """,
                (rs, rowNum) -> new PermissionRow(
                        rs.getString("code"),
                        rs.getString("group_code"),
                        rs.getString("name"),
                        rs.getString("description")
                )
        );
    }

    private List<String> groups(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT g.code
                FROM auth_groups g
                JOIN auth_user_group_memberships m ON m.group_id = g.id
                WHERE m.user_id = ?
                ORDER BY g.code
                """,
                (rs, rowNum) -> rs.getString("code"),
                userId
        );
    }

    private void ensureGroupsExist(List<String> groups) {
        for (String group : groups) {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM auth_groups WHERE code = ?)",
                    Boolean.class,
                    group
            );
            if (!Boolean.TRUE.equals(exists)) {
                throw new ResourceNotFoundException("Group not found");
            }
        }
    }

    private void ensureUserExists(UUID userId) {
        findUser(userId);
    }

    private void ensurePortfolioExists(UUID portfolioId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM portfolios WHERE id = ?)",
                Boolean.class,
                portfolioId
        );
        if (!Boolean.TRUE.equals(exists)) {
            throw new ResourceNotFoundException("Portfolio not found");
        }
    }

    private UserRow findUser(UUID userId) {
        List<UserRow> users = jdbcTemplate.query(
                """
                SELECT id, username, display_name, active, created_at, updated_at, last_login_at, portfolio_access_mode
                FROM auth_user_accounts
                WHERE id = ?
                """,
                (rs, rowNum) -> userRow(rs),
                userId
        );
        return users.stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private List<AdminWorkflowBookingResponse> workflowBookings(UUID portfolioId) {
        List<Object> params = new ArrayList<>();
        String where = "";
        if (portfolioId != null) {
            where = "WHERE portfolio_id = ?";
            params.add(portfolioId);
        }
        return jdbcTemplate.query(
                """
                SELECT id, portfolio_id, portfolio_name, status, underlying_symbol, option_type, strike,
                       maturity_date, quantity, submitted_by_display_name, submitted_at,
                       reviewed_by_display_name, reviewed_at, rejection_reason, confirmed_position_id
                FROM trade_booking_requests
                %s
                ORDER BY submitted_at DESC
                LIMIT 200
                """.formatted(where),
                this::workflowBooking,
                params.toArray()
        );
    }

    private List<AdminWorkflowBookingResponse> workflowLifecycleRequests(UUID portfolioId) {
        List<Object> params = new ArrayList<>();
        String where = "";
        if (portfolioId != null) {
            where = "WHERE portfolio_id = ?";
            params.add(portfolioId);
        }
        return jdbcTemplate.query(
                """
                SELECT id, portfolio_id, portfolio_name, status, request_type,
                       original_underlying_symbol, original_option_type, original_strike,
                       original_maturity_date, original_quantity, submitted_by_display_name,
                       submitted_at, reviewed_by_display_name, reviewed_at, rejection_reason,
                       resulting_position_id
                FROM trade_lifecycle_requests
                %s
                ORDER BY submitted_at DESC
                LIMIT 200
                """.formatted(where),
                this::workflowLifecycleRequest,
                params.toArray()
        );
    }

    private List<AdminWorkflowBookingResponse> filterByStatus(List<AdminWorkflowBookingResponse> bookings, String status) {
        return bookings.stream().filter(booking -> booking.status().equals(status)).toList();
    }

    private UserRow userRow(ResultSet rs) throws SQLException {
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        return new UserRow(
                rs.getObject("id", UUID.class),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                lastLogin == null ? null : lastLogin.toInstant(),
                rs.getString("portfolio_access_mode")
        );
    }

    private AdminPortfolioSummaryResponse portfolio(ResultSet rs, int rowNum) throws SQLException {
        return new AdminPortfolioSummaryResponse(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("base_currency"),
                rs.getLong("position_count")
        );
    }

    private AdminWorkflowBookingResponse workflowBooking(ResultSet rs, int rowNum) throws SQLException {
        Date maturityDate = rs.getDate("maturity_date");
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        String status = rs.getString("status");
        return new AdminWorkflowBookingResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("portfolio_id", UUID.class),
                rs.getString("portfolio_name"),
                status,
                status,
                rs.getString("underlying_symbol"),
                rs.getString("option_type"),
                rs.getBigDecimal("strike"),
                maturityDate == null ? null : maturityDate.toLocalDate(),
                rs.getBigDecimal("quantity"),
                rs.getString("submitted_by_display_name"),
                submittedAt == null ? null : submittedAt.toInstant(),
                rs.getString("reviewed_by_display_name"),
                reviewedAt == null ? null : reviewedAt.toInstant(),
                rs.getString("rejection_reason"),
                rs.getObject("confirmed_position_id", UUID.class)
        );
    }

    private AdminWorkflowBookingResponse workflowLifecycleRequest(ResultSet rs, int rowNum) throws SQLException {
        Date maturityDate = rs.getDate("original_maturity_date");
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        String status = rs.getString("status");
        String requestType = rs.getString("request_type");
        return new AdminWorkflowBookingResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("portfolio_id", UUID.class),
                rs.getString("portfolio_name"),
                switch (status) {
                    case "PENDING_VALIDATION" -> "LIFECYCLE_WAITING_BO";
                    case "APPROVED" -> "LIFECYCLE_APPROVED";
                    case "REJECTED" -> "LIFECYCLE_REJECTED";
                    default -> "LIFECYCLE_REQUESTED";
                },
                status,
                rs.getString("original_underlying_symbol"),
                requestType + " " + rs.getString("original_option_type"),
                rs.getBigDecimal("original_strike"),
                maturityDate == null ? null : maturityDate.toLocalDate(),
                rs.getBigDecimal("original_quantity"),
                rs.getString("submitted_by_display_name"),
                submittedAt == null ? null : submittedAt.toInstant(),
                rs.getString("reviewed_by_display_name"),
                reviewedAt == null ? null : reviewedAt.toInstant(),
                rs.getString("rejection_reason"),
                rs.getObject("resulting_position_id", UUID.class)
        );
    }

    private record UserRow(
            UUID id,
            String username,
            String displayName,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            Instant lastLoginAt,
            String portfolioAccessMode
    ) {
    }

    private record PermissionRow(String code, String groupCode, String name, String description) {
    }

    private record NodeDefinition(String id, String label, String description) {
    }
}

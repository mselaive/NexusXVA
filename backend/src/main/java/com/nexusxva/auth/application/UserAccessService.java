package com.nexusxva.auth.application;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.auth.infrastructure.JdbcUserAccessStore;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.shared.error.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccessService {

    private final JdbcUserAccessStore store;

    public UserAccessService(JdbcUserAccessStore store) {
        this.store = store;
    }

    @Transactional(readOnly = true)
    public void requireFeature(HttpServletRequest request, String permissionCode) {
        AuthSession session = currentSession(request);
        if (session == null) {
            return;
        }
        if (!store.isFeatureAllowed(session.user().id(), permissionCode)) {
            throw new AccessDeniedException("User is not allowed to use permission " + permissionCode);
        }
    }

    @Transactional(readOnly = true)
    public void requirePortfolioAccess(HttpServletRequest request, UUID portfolioId) {
        AuthSession session = currentSession(request);
        if (session == null) {
            return;
        }
        if (!store.canAccessPortfolio(session.user().id(), portfolioId)) {
            throw new AccessDeniedException("User is not allowed to access this portfolio");
        }
    }

    @Transactional(readOnly = true)
    public List<PortfolioSummary> filterVisiblePortfolios(HttpServletRequest request, List<PortfolioSummary> portfolios) {
        AuthSession session = currentSession(request);
        if (session == null || store.hasAllPortfolioAccess(session.user().id())) {
            return portfolios;
        }
        return portfolios.stream()
                .filter(portfolio -> store.canAccessPortfolio(session.user().id(), portfolio.id()))
                .toList();
    }

    @Transactional
    public void grantCreatedPortfolioIfNeeded(HttpServletRequest request, UUID portfolioId) {
        AuthSession session = currentSession(request);
        if (session == null || store.hasAllPortfolioAccess(session.user().id())) {
            return;
        }
        store.grantPortfolioAccess(session.user().id(), portfolioId, session);
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}

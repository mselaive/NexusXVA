package com.nexusxva.audit.infrastructure;

import com.nexusxva.audit.application.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnBean(AuditService.class)
public class AuditContextFilter extends OncePerRequestFilter {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final AuditService auditService;

    public AuditContextFilter(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String correlationId = UUID.randomUUID().toString();
        request.setAttribute(AuditService.CORRELATION_ID_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        MDC.put("correlationId", correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (isDenied(response.getStatus()) && !request.getRequestURI().startsWith("/api/auth")) {
                auditService.recordDenied(request, response.getStatus(), "Request denied by authorization layer");
            }
            MDC.clear();
        }
    }

    private boolean isDenied(int status) {
        return status == HttpServletResponse.SC_UNAUTHORIZED || status == HttpServletResponse.SC_FORBIDDEN;
    }
}

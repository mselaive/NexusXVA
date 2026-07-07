package com.nexusxva.operationalcontrol.application;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.application.AuthProperties;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.operationalcontrol.domain.OperationalControlSettings;
import com.nexusxva.operationalcontrol.domain.OperationalControlStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.time.DayOfWeek;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalControlService {

    private final OperationalControlStore store;
    private final AuditService auditService;
    private final AuthProperties authProperties;
    private final Clock clock;
    private final boolean enforcementEnabled;

    @Autowired
    public OperationalControlService(
            OperationalControlStore store,
            AuditService auditService,
            AuthProperties authProperties,
            @Value("${nexusxva.operational-control.enforcement.enabled:true}") boolean enforcementEnabled
    ) {
        this(store, auditService, authProperties, Clock.systemUTC(), enforcementEnabled);
    }

    OperationalControlService(
            OperationalControlStore store,
            AuditService auditService,
            AuthProperties authProperties,
            Clock clock
    ) {
        this(store, auditService, authProperties, clock, true);
    }

    OperationalControlService(
            OperationalControlStore store,
            AuditService auditService,
            AuthProperties authProperties,
            Clock clock,
            boolean enforcementEnabled
    ) {
        this.store = store;
        this.auditService = auditService;
        this.authProperties = authProperties;
        this.clock = clock;
        this.enforcementEnabled = enforcementEnabled;
    }

    @Transactional(readOnly = true)
    public OperationalControlSettings settings() {
        return store.settings();
    }

    @Transactional
    public OperationalControlSettings update(OperationalControlSettings settings, UUID updatedByUserId) {
        return store.save(settings, updatedByUserId);
    }

    @Transactional(readOnly = true)
    public OperationalControlStatus status() {
        return statusAt(clock.instant());
    }

    @Transactional(readOnly = true)
    public void ensureOpen(String action, AuthSession session, HttpServletRequest request) {
        if (!enforcementEnabled || !authProperties.isEnabled()) {
            return;
        }
        OperationalControlStatus status = status();
        if (status.tradingOpen()) {
            return;
        }
        Map<String, Object> metadata = closedMetadata(action, status);
        auditService.record(AuditEventCommand.of(
                "OPERATIONAL_WINDOW_DENIED",
                "OPERATIONAL_CONTROL",
                action,
                AuditOutcome.DENIED,
                session,
                request,
                409,
                null,
                null,
                "Operational window is closed",
                auditService.metadata(metadata)
        ));
        throw new OperationalWindowClosedException(metadata);
    }

    public OperationalControlStatus statusAt(Instant instant) {
        OperationalControlSettings settings = store.settings();
        ZonedDateTime now = instant.atZone(settings.timezone());
        boolean businessDay = settings.businessDays().contains(now.getDayOfWeek());
        LocalTime localTime = now.toLocalTime();
        boolean openTime = !localTime.isBefore(settings.tradingOpenTime())
                && localTime.isBefore(settings.tradingCloseTime());
        boolean tradingOpen = businessDay && openTime;
        String reason = tradingOpen
                ? "OPEN"
                : !businessDay ? "NON_BUSINESS_DAY" : localTime.isBefore(settings.tradingOpenTime()) ? "BEFORE_OPEN" : "AFTER_CLOSE";

        return new OperationalControlStatus(
                tradingOpen,
                reason,
                settings.timezone().getId(),
                now.toInstant(),
                nextOpenAt(settings, now).toInstant(),
                settings.tradingOpenTime(),
                settings.tradingCloseTime(),
                settings.eodEnabled(),
                nextEodAt(settings, now).toInstant()
        );
    }

    public LocalDate businessDate(Instant instant) {
        OperationalControlSettings settings = store.settings();
        return instant.atZone(settings.timezone()).toLocalDate();
    }

    private ZonedDateTime nextOpenAt(OperationalControlSettings settings, ZonedDateTime now) {
        for (int offset = 0; offset <= 14; offset++) {
            LocalDate candidate = now.toLocalDate().plusDays(offset);
            if (!settings.businessDays().contains(candidate.getDayOfWeek())) {
                continue;
            }
            ZonedDateTime open = LocalDateTime.of(candidate, settings.tradingOpenTime()).atZone(settings.timezone());
            if (open.isAfter(now)) {
                return open;
            }
            ZonedDateTime close = LocalDateTime.of(candidate, settings.tradingCloseTime()).atZone(settings.timezone());
            if (now.isBefore(close)) {
                return now;
            }
        }
        return LocalDateTime.of(now.toLocalDate().plusDays(1), settings.tradingOpenTime()).atZone(settings.timezone());
    }

    private ZonedDateTime nextEodAt(OperationalControlSettings settings, ZonedDateTime now) {
        for (int offset = 0; offset <= 14; offset++) {
            LocalDate candidate = now.toLocalDate().plusDays(offset);
            DayOfWeek day = candidate.getDayOfWeek();
            if (!settings.businessDays().contains(day)) {
                continue;
            }
            ZonedDateTime eod = LocalDateTime.of(candidate, settings.eodRunTime()).atZone(settings.timezone());
            if (eod.isAfter(now)) {
                return eod;
            }
        }
        return LocalDateTime.of(now.toLocalDate().plusDays(1), settings.eodRunTime()).atZone(settings.timezone());
    }

    private Map<String, Object> closedMetadata(String action, OperationalControlStatus status) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", action);
        metadata.put("timezone", status.timezone());
        metadata.put("currentBusinessTime", status.currentBusinessTime().toString());
        metadata.put("nextOpenAt", status.nextOpenAt().toString());
        metadata.put("tradingOpenTime", status.tradingOpenTime().toString());
        metadata.put("tradingCloseTime", status.tradingCloseTime().toString());
        metadata.put("reason", status.reason());
        return metadata;
    }
}

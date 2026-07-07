package com.nexusxva.operationalcontrol.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.application.AuditStore;
import com.nexusxva.auth.application.AuthProperties;
import com.nexusxva.operationalcontrol.domain.OperationalControlSettings;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperationalControlServiceTest {

    @Mock
    private OperationalControlStore store;

    @Mock
    private AuditStore auditStore;

    private AuthProperties authProperties;

    private OperationalControlService service;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        service = new OperationalControlService(
                store,
                new AuditService(auditStore, new ObjectMapper()),
                authProperties,
                Clock.fixed(Instant.parse("2026-07-06T21:00:00Z"), ZoneId.of("UTC"))
        );
        lenient().when(store.settings()).thenReturn(settings());
    }

    @Test
    void reportsOpenDuringConfiguredBusinessWindow() {
        var status = service.statusAt(Instant.parse("2026-07-06T14:00:00Z"));

        assertThat(status.tradingOpen()).isTrue();
        assertThat(status.reason()).isEqualTo("OPEN");
    }

    @Test
    void reportsClosedAfterConfiguredBusinessWindow() {
        var status = service.statusAt(Instant.parse("2026-07-06T21:00:00Z"));

        assertThat(status.tradingOpen()).isFalse();
        assertThat(status.reason()).isEqualTo("AFTER_CLOSE");
        assertThat(status.nextOpenAt()).isEqualTo(Instant.parse("2026-07-07T13:30:00Z"));
    }

    @Test
    void ensureOpenDoesNothingWhenAuthIsDisabled() {
        assertThatCode(() -> service.ensureOpen("RUN_CVA", null, null)).doesNotThrowAnyException();
    }

    @Test
    void ensureOpenRejectsWhenAuthIsEnabledAndWindowIsClosed() {
        authProperties.setEnabled(true);

        assertThatThrownBy(() -> service.ensureOpen("RUN_CVA", null, null))
                .isInstanceOf(OperationalWindowClosedException.class)
                .hasMessage("Operational window is closed");
    }

    @Test
    void ensureOpenCanBeDisabledForControlledTestEnvironments() {
        authProperties.setEnabled(true);
        OperationalControlService disabledService = new OperationalControlService(
                store,
                new AuditService(auditStore, new ObjectMapper()),
                authProperties,
                Clock.fixed(Instant.parse("2026-07-06T21:00:00Z"), ZoneId.of("UTC")),
                false
        );

        assertThatCode(() -> disabledService.ensureOpen("RUN_CVA", null, null)).doesNotThrowAnyException();
    }

    private OperationalControlSettings settings() {
        return new OperationalControlSettings(
                ZoneId.of("America/New_York"),
                EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                LocalTime.of(9, 30),
                LocalTime.of(16, 0),
                false,
                LocalTime.of(17, 15),
                false,
                Instant.parse("2026-07-06T12:00:00Z"),
                null,
                0
        );
    }
}

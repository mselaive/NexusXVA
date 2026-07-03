package com.nexusxva.audit.application;

import com.nexusxva.audit.domain.AuditEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;

public interface AuditStore {

    void save(AuditEvent event);

    Optional<AuditEvent> find(UUID eventId);

    Page<AuditEvent> search(AuditSearchCriteria criteria);
}

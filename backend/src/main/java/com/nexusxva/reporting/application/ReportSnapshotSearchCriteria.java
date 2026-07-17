package com.nexusxva.reporting.application;

import com.nexusxva.reporting.domain.ReportSnapshotType;
import java.util.UUID;

public record ReportSnapshotSearchCriteria(
        ReportSnapshotType reportType,
        UUID requestedByUserId,
        String activeGroupCode,
        int limit
) {
    public ReportSnapshotSearchCriteria {
        if (limit <= 0 || limit > 200) {
            limit = 50;
        }
    }
}

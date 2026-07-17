package com.nexusxva.reporting.application;

import com.nexusxva.reporting.domain.ReportSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportSnapshotStore {

    ReportSnapshot save(ReportSnapshot snapshot);

    Optional<ReportSnapshot> find(UUID snapshotId);

    List<ReportSnapshot> search(ReportSnapshotSearchCriteria criteria);
}

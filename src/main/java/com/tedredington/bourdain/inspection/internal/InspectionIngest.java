package com.tedredington.bourdain.inspection.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.tedredington.bourdain.civicdata.InspectionBatchReceived;
import com.tedredington.bourdain.civicdata.InspectionRecord;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Writes inspection pages inside the sync's page transaction. Idempotent: a
 * page's existing inspections load in one query, and dirty checking writes
 * only what changed. When the city amends an inspection, the version it
 * replaces is kept as a revision.
 */
@Component
class InspectionIngest {

    private final InspectionRepository inspections;
    private final InspectionRevisionRepository revisions;
    private final Clock clock;

    InspectionIngest(InspectionRepository inspections, InspectionRevisionRepository revisions, Clock clock) {
        this.inspections = inspections;
        this.revisions = revisions;
        this.clock = clock;
    }

    @EventListener
    void on(InspectionBatchReceived batch) {
        Map<Long, Inspection> known = new HashMap<>();
        inspections.findAllById(batch.records().stream().map(InspectionRecord::inspectionId).distinct().toList())
                .forEach(inspection -> known.put(inspection.getId(), inspection));

        Instant now = clock.instant();
        for (InspectionRecord record : batch.records()) {
            Inspection inspection = known.get(record.inspectionId());
            if (inspection == null) {
                known.put(record.inspectionId(), inspections.save(Inspection.published(record, now)));
                continue;
            }
            if (inspection.isAmendedBy(record)) {
                revisions.save(new InspectionRevision(inspection, now));
            }
            inspection.update(record, now);
        }
    }
}

package com.tedredington.bourdain.establishment.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tedredington.bourdain.civicdata.InspectionBatchReceived;
import com.tedredington.bourdain.civicdata.InspectionRecord;
import com.tedredington.bourdain.civicdata.LicenseBatchReceived;
import com.tedredington.bourdain.civicdata.LicenseRecord;
import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.InspectionResult;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Maintains the establishment row per license from the inspection stream, and
 * mirrors active business licenses. Each page's existing rows load in one
 * query; dirty checking then writes only what changed. An establishment
 * reflects its most recent inspection regardless of batch order, and a newer
 * inspection that changes its details (often a new owner on a reused license
 * number) keeps the details it replaces as a revision.
 */
@Component
class EstablishmentIngest {

    private final EstablishmentRepository establishments;
    private final EstablishmentRevisionRepository revisions;
    private final BusinessLicenseRepository licenses;
    private final Clock clock;

    EstablishmentIngest(EstablishmentRepository establishments, EstablishmentRevisionRepository revisions,
                        BusinessLicenseRepository licenses, Clock clock) {
        this.establishments = establishments;
        this.revisions = revisions;
        this.licenses = licenses;
        this.clock = clock;
    }

    @EventListener
    void on(InspectionBatchReceived batch) {
        Map<Long, Establishment> known = new HashMap<>();
        establishments.findAllById(batch.records().stream().map(InspectionRecord::licenseNumber).distinct().toList())
                .forEach(establishment -> known.put(establishment.getId(), establishment));

        Instant now = clock.instant();
        for (InspectionRecord record : batch.records()) {
            EstablishmentSnapshot snapshot = snapshot(record);
            Establishment establishment = known.get(snapshot.licenseNumber());
            if (establishment == null) {
                known.put(snapshot.licenseNumber(), establishments.save(Establishment.firstSeen(snapshot, now)));
            } else if (establishment.isSupersededBy(snapshot)) {
                if (establishment.detailsDifferFrom(snapshot)) {
                    revisions.save(new EstablishmentRevision(establishment, now));
                }
                establishment.apply(snapshot, now);
            }
        }
    }

    @EventListener
    void on(LicenseBatchReceived batch) {
        List<String> recordIds = batch.records().stream().map(LicenseRecord::recordId).distinct().toList();
        Map<String, BusinessLicense> known = new HashMap<>();
        licenses.findAllById(recordIds).forEach(license -> known.put(license.getId(), license));

        Instant now = clock.instant();
        for (LicenseRecord record : batch.records()) {
            BusinessLicense license = known.get(record.recordId());
            if (license == null) {
                known.put(record.recordId(), licenses.save(BusinessLicense.listed(record, batch.syncRunId(), now)));
            } else {
                license.list(record, batch.syncRunId(), now);
            }
        }
    }

    private static EstablishmentSnapshot snapshot(InspectionRecord r) {
        return new EstablishmentSnapshot(
                r.licenseNumber(),
                r.inspectionId(),
                r.dbaName(),
                NameNormalizer.normalize(r.dbaName()),
                r.akaName(),
                r.facilityType(),
                FacilityCategory.classify(r.facilityType()),
                Risk.fromRaw(r.risk()),
                r.address() == null ? "" : r.address(),
                r.city(),
                r.state(),
                r.zip(),
                r.latitude(),
                r.longitude(),
                InspectionResult.fromRaw(r.result()),
                r.inspectedOn());
    }
}

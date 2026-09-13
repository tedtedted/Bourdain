package com.tedredington.bourdain.establishment.internal;

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
 * mirrors active business licenses. An establishment reflects its most recent
 * inspection regardless of batch order; a newer inspection that changes its
 * details (often a new owner on a reused license number) keeps the details it
 * replaces as a revision.
 */
@Component
class EstablishmentIngest {

    private final EstablishmentRepository establishments;
    private final BusinessLicenseRepository licenses;

    EstablishmentIngest(EstablishmentRepository establishments, BusinessLicenseRepository licenses) {
        this.establishments = establishments;
        this.licenses = licenses;
    }

    @EventListener
    void on(InspectionBatchReceived batch) {
        establishments.upsertLatest(batch.records().stream().map(EstablishmentIngest::snapshot).toList());
    }

    @EventListener
    void on(LicenseBatchReceived batch) {
        licenses.upsertAll(batch.records().stream().map(EstablishmentIngest::license).toList());
    }

    private static EstablishmentSnapshot snapshot(InspectionRecord r) {
        return new EstablishmentSnapshot(
                r.licenseNumber(),
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

    private static BusinessLicense license(LicenseRecord r) {
        return new BusinessLicense(
                r.recordId(),
                r.licenseNumber(),
                r.dbaName(),
                NameNormalizer.normalize(r.dbaName()),
                r.legalName(),
                r.licenseDescription(),
                r.address(),
                r.city(),
                r.state(),
                r.zip(),
                r.licenseStartDate(),
                r.expirationDate(),
                r.statusRaw(),
                r.latitude(),
                r.longitude());
    }
}

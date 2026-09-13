package com.tedredington.bourdain.inspection.internal;

import com.tedredington.bourdain.civicdata.InspectionBatchReceived;
import com.tedredington.bourdain.civicdata.InspectionRecord;
import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.InspectionType;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Writes inspection pages inside the sync's page transaction. */
@Component
class InspectionIngest {

    private final InspectionRepository inspections;

    InspectionIngest(InspectionRepository inspections) {
        this.inspections = inspections;
    }

    @EventListener
    void on(InspectionBatchReceived batch) {
        inspections.upsertAll(batch.records().stream().map(InspectionIngest::inspection).toList());
    }

    private static Inspection inspection(InspectionRecord r) {
        return new Inspection(
                r.inspectionId(),
                r.licenseNumber(),
                r.dbaName(),
                r.inspectedOn(),
                InspectionResult.fromRaw(r.result()),
                InspectionType.classify(r.inspectionType()),
                r.inspectionType(),
                r.violations(),
                ViolationParser.parse(r.violations()).stream().map(Violation::of).toList());
    }
}

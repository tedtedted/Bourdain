package com.tedredington.bourdain.inspection;

import java.time.LocalDate;
import java.util.List;

/** Read-side API of the inspection module. */
public interface Inspections {

    /** Most recent failed inspections, newest first — the homepage feed. */
    List<RecentFailure> recentFailures(int limit);

    /** Full inspection history for a license, newest first, violations included. */
    List<InspectionDetail> history(long licenseNumber);

    record RecentFailure(
            long inspectionId,
            long licenseNumber,
            String dbaName,
            LocalDate inspectedOn,
            String headline,
            int violationCount) {
    }

    record InspectionDetail(
            long inspectionId,
            LocalDate inspectedOn,
            InspectionType type,
            String typeRaw,
            InspectionResult result,
            List<ViolationLine> violations) {
    }

    record ViolationLine(int code, String description, String comment) {
    }
}

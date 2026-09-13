package com.tedredington.bourdain.inspection.internal;

import java.time.LocalDate;
import java.util.List;

import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.InspectionType;
import com.tedredington.bourdain.inspection.Inspections.ViolationLine;

/** One inspection as the city published it, with its violations parsed in order. */
record Inspection(
        long id,
        long licenseNumber,
        String dbaName,
        LocalDate inspectedOn,
        InspectionResult result,
        InspectionType type,
        String typeRaw,
        String violationsRaw,
        List<ViolationLine> violations) {
}

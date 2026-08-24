package com.tedredington.bourdain.civicdata;

import java.time.LocalDate;

/**
 * One validated row of the Food Inspections dataset. Rows without a usable
 * license number, inspection id, or date are dropped during mapping.
 */
public record InspectionRecord(
        long inspectionId,
        long licenseNumber,
        String dbaName,
        String akaName,
        String facilityType,
        String risk,
        String address,
        String city,
        String state,
        String zip,
        LocalDate inspectedOn,
        String inspectionType,
        String result,
        String violations,
        Double latitude,
        Double longitude) {
}

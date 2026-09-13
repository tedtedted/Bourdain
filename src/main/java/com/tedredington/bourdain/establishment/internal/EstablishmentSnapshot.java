package com.tedredington.bourdain.establishment.internal;

import java.time.LocalDate;

import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.InspectionResult;

/** An establishment's details as one inspection recorded them. */
record EstablishmentSnapshot(
        long licenseNumber,
        long inspectionId,
        String name,
        String normalizedName,
        String akaName,
        String facilityTypeRaw,
        FacilityCategory facilityCategory,
        Risk risk,
        String address,
        String city,
        String state,
        String zip,
        Double latitude,
        Double longitude,
        InspectionResult latestResult,
        LocalDate lastInspectedOn) {
}

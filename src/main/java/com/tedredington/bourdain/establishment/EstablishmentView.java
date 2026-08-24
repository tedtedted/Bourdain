package com.tedredington.bourdain.establishment;

import java.time.LocalDate;

import com.tedredington.bourdain.inspection.InspectionResult;

/** Read model of an establishment as the web layer sees it. */
public record EstablishmentView(
        long licenseNumber,
        String name,
        String akaName,
        String address,
        String city,
        String zip,
        FacilityCategory category,
        String facilityTypeRaw,
        Risk risk,
        EstablishmentStatus status,
        InspectionResult latestResult,
        LocalDate lastInspectedOn,
        Relocation relocation) {

    /** Present only when {@link #status()} is {@link EstablishmentStatus#RELOCATED}. */
    public record Relocation(long licenseNumber, String address, LocalDate since) {
    }
}

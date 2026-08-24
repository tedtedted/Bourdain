package com.tedredington.bourdain.civicdata;

import java.time.LocalDate;

/** One active business license row, pre-filtered to food-related license types. */
public record LicenseRecord(
        String recordId,
        long licenseNumber,
        String dbaName,
        String legalName,
        String licenseDescription,
        String address,
        String city,
        String state,
        String zip,
        LocalDate licenseStartDate,
        LocalDate expirationDate,
        String statusRaw,
        Double latitude,
        Double longitude) {
}

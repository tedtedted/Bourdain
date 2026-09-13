package com.tedredington.bourdain.establishment.internal;

import java.time.LocalDate;

/** One row of the active business license mirror. */
record BusinessLicense(
        String recordId,
        long licenseNumber,
        String dbaName,
        String normalizedName,
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

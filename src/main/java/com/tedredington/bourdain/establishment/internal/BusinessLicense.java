package com.tedredington.bourdain.establishment.internal;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** One row of the active business license mirror. */
@Table("business_license")
record BusinessLicense(
        @Id String recordId,
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

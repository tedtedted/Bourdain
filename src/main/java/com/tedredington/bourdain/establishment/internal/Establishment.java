package com.tedredington.bourdain.establishment.internal;

import java.time.LocalDate;

import com.tedredington.bourdain.establishment.EstablishmentStatus;
import com.tedredington.bourdain.establishment.EstablishmentView;
import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.InspectionResult;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One row per city license number. Only ever written through the SQL in
 * {@link EstablishmentRepositoryCustom}; this aggregate exists to back reads.
 */
@Table("establishment")
record Establishment(
        @Id long licenseNumber,
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
        EstablishmentStatus status,
        InspectionResult latestResult,
        LocalDate lastInspectedOn,
        Long relocatedToLicenseNumber,
        String relocatedToAddress,
        LocalDate relocatedSince) {

    EstablishmentView toView() {
        EstablishmentView.Relocation relocation = null;
        if (status == EstablishmentStatus.RELOCATED && relocatedToLicenseNumber != null) {
            relocation = new EstablishmentView.Relocation(relocatedToLicenseNumber, relocatedToAddress, relocatedSince);
        }
        return new EstablishmentView(licenseNumber, name, akaName, address, city, zip,
                facilityCategory, facilityTypeRaw, risk, status, latestResult, lastInspectedOn, relocation);
    }
}

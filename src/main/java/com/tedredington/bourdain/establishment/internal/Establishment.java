package com.tedredington.bourdain.establishment.internal;

import java.time.LocalDate;

import com.tedredington.bourdain.establishment.EstablishmentStatus;
import com.tedredington.bourdain.establishment.EstablishmentView;
import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.InspectionResult;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA view of the {@code establishment} table. Rows are written by
 * {@link EstablishmentIngest} (bulk SQL upserts) and
 * {@link RelocationService}; this entity is read-only in practice and only
 * exists to back queries.
 */
@Entity
@Table(name = "establishment")
class Establishment {

    @Id
    private Long licenseNumber;

    private String name;
    private String normalizedName;
    private String akaName;
    private String facilityTypeRaw;

    @Enumerated(EnumType.STRING)
    private FacilityCategory facilityCategory;

    @Enumerated(EnumType.STRING)
    private Risk risk;

    private String address;
    private String city;
    private String state;
    private String zip;
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    private EstablishmentStatus status;

    @Enumerated(EnumType.STRING)
    private InspectionResult latestResult;

    private LocalDate lastInspectedOn;
    private Long relocatedToLicenseNumber;
    private String relocatedToAddress;
    private LocalDate relocatedSince;

    protected Establishment() {
    }

    EstablishmentView toView() {
        EstablishmentView.Relocation relocation = null;
        if (status == EstablishmentStatus.RELOCATED && relocatedToLicenseNumber != null) {
            relocation = new EstablishmentView.Relocation(relocatedToLicenseNumber, relocatedToAddress, relocatedSince);
        }
        return new EstablishmentView(licenseNumber, name, akaName, address, city, zip,
                facilityCategory, facilityTypeRaw, risk, status, latestResult, lastInspectedOn, relocation);
    }
}

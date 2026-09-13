package com.tedredington.bourdain.establishment.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.tedredington.bourdain.establishment.EstablishmentStatus;
import com.tedredington.bourdain.establishment.EstablishmentView;
import com.tedredington.bourdain.establishment.EstablishmentView.Relocation;
import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.InspectionResult;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

/**
 * One row per city license number, reflecting the most recent inspection
 * under it. The license number comes from the city, so {@link Persistable}
 * tells Spring Data which rows are new rather than it querying for each.
 */
@Entity
@Table(name = "establishment")
class Establishment implements Persistable<Long> {

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
    private Long latestInspectionId;
    private Long relocatedToLicenseNumber;
    private String relocatedToAddress;
    private LocalDate relocatedSince;
    private Instant createdAt;
    private Instant updatedAt;

    @Transient
    private boolean isNew = true;

    protected Establishment() {
    }

    static Establishment firstSeen(EstablishmentSnapshot snapshot, Instant now) {
        Establishment establishment = new Establishment();
        establishment.licenseNumber = snapshot.licenseNumber();
        establishment.status = EstablishmentStatus.OPEN;
        establishment.createdAt = now;
        establishment.apply(snapshot, now);
        return establishment;
    }

    /**
     * Whether the snapshot comes from this establishment's most recent
     * inspection: a later date, or the same date and an inspection id at least
     * as high. The id settles same-day inspections that record different
     * details; without it, each replay would flip between them.
     */
    boolean isSupersededBy(EstablishmentSnapshot snapshot) {
        int byDate = snapshot.lastInspectedOn().compareTo(lastInspectedOn == null ? LocalDate.MIN : lastInspectedOn);
        if (byDate != 0) {
            return byDate > 0;
        }
        return latestInspectionId == null || snapshot.inspectionId() >= latestInspectionId;
    }

    /** The details worth archiving when they change: who and where, not derived columns. */
    boolean detailsDifferFrom(EstablishmentSnapshot snapshot) {
        return !Objects.equals(name, snapshot.name())
                || !Objects.equals(akaName, snapshot.akaName())
                || !Objects.equals(facilityTypeRaw, snapshot.facilityTypeRaw())
                || risk != snapshot.risk()
                || !Objects.equals(address, snapshot.address())
                || !Objects.equals(city, snapshot.city())
                || !Objects.equals(state, snapshot.state())
                || !Objects.equals(zip, snapshot.zip())
                || !Objects.equals(latitude, snapshot.latitude())
                || !Objects.equals(longitude, snapshot.longitude());
    }

    /** Applies the snapshot if anything it carries differs; unchanged rows stay untouched. */
    void apply(EstablishmentSnapshot snapshot, Instant now) {
        if (!isNew && !detailsDifferFrom(snapshot)
                && Objects.equals(normalizedName, snapshot.normalizedName())
                && facilityCategory == snapshot.facilityCategory()
                && latestResult == snapshot.latestResult()
                && Objects.equals(lastInspectedOn, snapshot.lastInspectedOn())
                && Objects.equals(latestInspectionId, snapshot.inspectionId())) {
            return;
        }
        name = snapshot.name();
        normalizedName = snapshot.normalizedName();
        akaName = snapshot.akaName();
        facilityTypeRaw = snapshot.facilityTypeRaw();
        facilityCategory = snapshot.facilityCategory();
        risk = snapshot.risk();
        address = snapshot.address();
        city = snapshot.city();
        state = snapshot.state();
        zip = snapshot.zip();
        latitude = snapshot.latitude();
        longitude = snapshot.longitude();
        latestResult = snapshot.latestResult();
        lastInspectedOn = snapshot.lastInspectedOn();
        latestInspectionId = snapshot.inspectionId();
        updatedAt = now;
    }

    void relocateTo(Relocation relocation, Instant now) {
        status = EstablishmentStatus.RELOCATED;
        relocatedToLicenseNumber = relocation.licenseNumber();
        relocatedToAddress = relocation.address();
        relocatedSince = relocation.since();
        updatedAt = now;
    }

    EstablishmentView toView() {
        Relocation relocation = null;
        if (status == EstablishmentStatus.RELOCATED && relocatedToLicenseNumber != null) {
            relocation = new Relocation(relocatedToLicenseNumber, relocatedToAddress, relocatedSince);
        }
        return new EstablishmentView(licenseNumber, name, akaName, address, city, zip,
                facilityCategory, facilityTypeRaw, risk, status, latestResult, lastInspectedOn, relocation);
    }

    @Override
    public Long getId() {
        return licenseNumber;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        isNew = false;
    }

    String name() {
        return name;
    }

    String akaName() {
        return akaName;
    }

    String facilityTypeRaw() {
        return facilityTypeRaw;
    }

    FacilityCategory facilityCategory() {
        return facilityCategory;
    }

    Risk risk() {
        return risk;
    }

    String address() {
        return address;
    }

    String city() {
        return city;
    }

    String state() {
        return state;
    }

    String zip() {
        return zip;
    }

    Double latitude() {
        return latitude;
    }

    Double longitude() {
        return longitude;
    }

    LocalDate lastInspectedOn() {
        return lastInspectedOn;
    }
}

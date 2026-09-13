package com.tedredington.bourdain.establishment.internal;

import java.time.Instant;
import java.time.LocalDate;

import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Risk;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * What an establishment looked like before a newer inspection changed its
 * details, e.g. the previous owner of a reused license number.
 */
@Entity
@Table(name = "establishment_revision")
class EstablishmentRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "establishment_revision_id_seq")
    @SequenceGenerator(name = "establishment_revision_id_seq", sequenceName = "establishment_revision_id_seq",
            allocationSize = 50)
    private Long id;

    // An association rather than a plain column, so a revision of an
    // establishment first seen in the same flush is inserted after it.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "license_number")
    private Establishment establishment;

    private String name;
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
    private LocalDate lastInspectedOn;
    private Instant supersededAt;

    protected EstablishmentRevision() {
    }

    /** Captures the establishment as it is now; call before applying the newer details. */
    EstablishmentRevision(Establishment replaced, Instant supersededAt) {
        this.establishment = replaced;
        this.name = replaced.name();
        this.akaName = replaced.akaName();
        this.facilityTypeRaw = replaced.facilityTypeRaw();
        this.facilityCategory = replaced.facilityCategory();
        this.risk = replaced.risk();
        this.address = replaced.address();
        this.city = replaced.city();
        this.state = replaced.state();
        this.zip = replaced.zip();
        this.latitude = replaced.latitude();
        this.longitude = replaced.longitude();
        this.lastInspectedOn = replaced.lastInspectedOn();
        this.supersededAt = supersededAt;
    }
}

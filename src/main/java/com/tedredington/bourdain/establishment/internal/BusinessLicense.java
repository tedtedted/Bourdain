package com.tedredington.bourdain.establishment.internal;

import java.time.Instant;
import java.time.LocalDate;

import com.tedredington.bourdain.civicdata.LicenseRecord;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

/** One row of the active business license mirror, keyed by the city's record id. */
@Entity
@Table(name = "business_license")
class BusinessLicense implements Persistable<String> {

    @Id
    private String recordId;

    private long licenseNumber;
    private String dbaName;
    private String normalizedName;
    private String legalName;
    private String licenseDescription;
    private String address;
    private String city;
    private String state;
    private String zip;
    private LocalDate licenseStartDate;
    private LocalDate expirationDate;
    private String statusRaw;
    private Double latitude;
    private Double longitude;
    private Instant updatedAt;
    private Instant delistedAt;
    private Long lastSeenSyncRunId;

    @Transient
    private boolean isNew = true;

    protected BusinessLicense() {
    }

    static BusinessLicense listed(LicenseRecord record, long syncRunId, Instant now) {
        BusinessLicense license = new BusinessLicense();
        license.recordId = record.recordId();
        license.list(record, syncRunId, now);
        return license;
    }

    /** Refreshes the details and records that this run listed the license, listing it again if it had lapsed. */
    void list(LicenseRecord record, long syncRunId, Instant now) {
        licenseNumber = record.licenseNumber();
        dbaName = record.dbaName();
        normalizedName = NameNormalizer.normalize(record.dbaName());
        legalName = record.legalName();
        licenseDescription = record.licenseDescription();
        address = record.address();
        city = record.city();
        state = record.state();
        zip = record.zip();
        licenseStartDate = record.licenseStartDate();
        expirationDate = record.expirationDate();
        statusRaw = record.statusRaw();
        latitude = record.latitude();
        longitude = record.longitude();
        delistedAt = null;
        lastSeenSyncRunId = syncRunId;
        updatedAt = now;
    }

    @Override
    public String getId() {
        return recordId;
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
}

package com.tedredington.bourdain.inspection.internal;

import java.time.Instant;
import java.time.LocalDate;

import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.InspectionType;

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

/** The version of an inspection the city replaced. Violations can be re-parsed from {@code violationsRaw}. */
@Entity
@Table(name = "inspection_revision")
class InspectionRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inspection_revision_id_seq")
    @SequenceGenerator(name = "inspection_revision_id_seq", sequenceName = "inspection_revision_id_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    private long licenseNumber;
    private String dbaName;
    private LocalDate inspectedOn;

    @Enumerated(EnumType.STRING)
    private InspectionResult result;

    @Enumerated(EnumType.STRING)
    private InspectionType inspectionType;

    private String inspectionTypeRaw;
    private String violationsRaw;
    private Instant supersededAt;

    protected InspectionRevision() {
    }

    /** Captures the inspection as it is now; call before applying the amendment. */
    InspectionRevision(Inspection replaced, Instant supersededAt) {
        this.inspection = replaced;
        this.licenseNumber = replaced.licenseNumber();
        this.dbaName = replaced.dbaName();
        this.inspectedOn = replaced.inspectedOn();
        this.result = replaced.result();
        this.inspectionType = replaced.type();
        this.inspectionTypeRaw = replaced.inspectionTypeRaw();
        this.violationsRaw = replaced.violationsRaw();
        this.supersededAt = supersededAt;
    }
}

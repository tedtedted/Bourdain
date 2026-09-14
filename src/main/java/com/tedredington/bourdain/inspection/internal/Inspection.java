package com.tedredington.bourdain.inspection.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tedredington.bourdain.civicdata.InspectionRecord;
import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.InspectionType;
import com.tedredington.bourdain.inspection.Inspections.InspectionDetail;
import com.tedredington.bourdain.inspection.Inspections.ViolationLine;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

/**
 * One inspection as the city published it, with its violations parsed in
 * order. The id is the city's, so {@link Persistable} tells Spring Data which
 * inspections are new rather than it querying for each.
 */
@Entity
@Table(name = "inspection")
class Inspection implements Persistable<Long> {

    @Id
    private Long id;

    private long licenseNumber;
    private String dbaName;
    private LocalDate inspectedOn;

    @Enumerated(EnumType.STRING)
    private InspectionResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_type")
    private InspectionType type;

    private String inspectionTypeRaw;
    private String violationsRaw;
    private Instant updatedAt;

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordinal")
    private List<Violation> violations = new ArrayList<>();

    @Transient
    private boolean isNew = true;

    protected Inspection() {
    }

    static Inspection published(InspectionRecord record, Instant now) {
        Inspection inspection = new Inspection();
        inspection.id = record.inspectionId();
        inspection.update(record, now);
        return inspection;
    }

    /** Whether the city changed what it published, as opposed to Bourdain reclassifying it. */
    boolean isAmendedBy(InspectionRecord record) {
        return licenseNumber != record.licenseNumber()
                || !Objects.equals(dbaName, record.dbaName())
                || !Objects.equals(inspectedOn, record.inspectedOn())
                || result != InspectionResult.fromRaw(record.result())
                || !Objects.equals(inspectionTypeRaw, record.inspectionType())
                || !Objects.equals(violationsRaw, record.violations());
    }

    /**
     * Brings the inspection in line with the record. Violations are re-parsed
     * only when their raw text changed, and an unchanged inspection isn't
     * written at all.
     */
    void update(InspectionRecord record, Instant now) {
        InspectionType classified = InspectionType.classify(record.inspectionType());
        boolean violationsChanged = isNew || !Objects.equals(violationsRaw, record.violations());
        if (!isNew && !isAmendedBy(record) && type == classified) {
            return;
        }
        licenseNumber = record.licenseNumber();
        dbaName = record.dbaName();
        inspectedOn = record.inspectedOn();
        result = InspectionResult.fromRaw(record.result());
        type = classified;
        inspectionTypeRaw = record.inspectionType();
        violationsRaw = record.violations();
        updatedAt = now;
        if (violationsChanged) {
            violations.clear();
            List<ViolationLine> lines = ViolationParser.parse(record.violations());
            for (int ordinal = 0; ordinal < lines.size(); ordinal++) {
                violations.add(new Violation(this, ordinal, lines.get(ordinal)));
            }
        }
    }

    InspectionDetail toDetail() {
        return new InspectionDetail(id, inspectedOn, type, inspectionTypeRaw, result,
                violations.stream().map(Violation::toLine).toList());
    }

    @Override
    public Long getId() {
        return id;
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

    long licenseNumber() {
        return licenseNumber;
    }

    String dbaName() {
        return dbaName;
    }

    LocalDate inspectedOn() {
        return inspectedOn;
    }

    InspectionResult result() {
        return result;
    }

    InspectionType type() {
        return type;
    }

    String inspectionTypeRaw() {
        return inspectionTypeRaw;
    }

    String violationsRaw() {
        return violationsRaw;
    }
}

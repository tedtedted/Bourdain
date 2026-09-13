package com.tedredington.bourdain.inspection.internal;

import com.tedredington.bourdain.inspection.Inspections.ViolationLine;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/** Part of an {@link Inspection}; {@code ordinal} is its position in the inspector's list. */
@Entity
@Table(name = "violation")
class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "violation_id_seq")
    @SequenceGenerator(name = "violation_id_seq", sequenceName = "violation_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    private int ordinal;
    private int code;
    private String description;
    private String comment;

    protected Violation() {
    }

    Violation(Inspection inspection, int ordinal, ViolationLine line) {
        this.inspection = inspection;
        this.ordinal = ordinal;
        this.code = line.code();
        this.description = line.description();
        this.comment = line.comment();
    }

    ViolationLine toLine() {
        return new ViolationLine(code, description, comment);
    }
}

package com.tedredington.bourdain.inspection.internal;

import java.time.LocalDate;
import java.util.List;

import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.InspectionType;
import com.tedredington.bourdain.inspection.Inspections.InspectionDetail;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/** One inspection as the city published it, with its violations parsed in order. */
@Table("inspection")
record Inspection(
        @Id long id,
        long licenseNumber,
        String dbaName,
        LocalDate inspectedOn,
        InspectionResult result,
        @Column("inspection_type") InspectionType type,
        @Column("inspection_type_raw") String typeRaw,
        String violationsRaw,
        @MappedCollection(idColumn = "inspection_id", keyColumn = "ordinal") List<Violation> violations) {

    InspectionDetail toDetail() {
        return new InspectionDetail(id, inspectedOn, type, typeRaw, result,
                violations.stream().map(Violation::toLine).toList());
    }
}

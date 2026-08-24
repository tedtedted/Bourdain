package com.tedredington.bourdain.inspection.internal;

import java.util.ArrayList;
import java.util.List;

import com.tedredington.bourdain.civicdata.InspectionBatchReceived;
import com.tedredington.bourdain.civicdata.InspectionRecord;
import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.InspectionType;
import com.tedredington.bourdain.inspection.Inspections.ViolationLine;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * Writes inspection pages inside the sync's page transaction. Idempotent:
 * re-running a page upserts the same rows and rebuilds their violations.
 */
@Component
class InspectionIngest {

    private static final String UPSERT = """
            insert into inspection (id, license_number, dba_name, inspected_on, result,
                                    inspection_type, inspection_type_raw, violations_raw, updated_at)
            values (:id, :licenseNumber, :dbaName, :inspectedOn, :result, :type, :typeRaw, :violationsRaw, now())
            on conflict (id) do update set
                license_number = excluded.license_number,
                dba_name = excluded.dba_name,
                inspected_on = excluded.inspected_on,
                result = excluded.result,
                inspection_type = excluded.inspection_type,
                inspection_type_raw = excluded.inspection_type_raw,
                violations_raw = excluded.violations_raw,
                updated_at = now()
            """;

    private final NamedParameterJdbcTemplate jdbc;

    InspectionIngest(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener
    void on(InspectionBatchReceived batch) {
        List<InspectionRecord> records = batch.records();

        SqlParameterSource[] rows = records.stream()
                .map(r -> new MapSqlParameterSource()
                        .addValue("id", r.inspectionId())
                        .addValue("licenseNumber", r.licenseNumber())
                        .addValue("dbaName", r.dbaName())
                        .addValue("inspectedOn", r.inspectedOn())
                        .addValue("result", InspectionResult.fromRaw(r.result()).name())
                        .addValue("type", InspectionType.classify(r.inspectionType()).name())
                        .addValue("typeRaw", r.inspectionType())
                        .addValue("violationsRaw", r.violations()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(UPSERT, rows);

        // Rebuild violations wholesale for the affected inspections; parsing is
        // deterministic, so delete + insert is the simplest idempotent shape.
        List<Long> ids = records.stream().map(InspectionRecord::inspectionId).toList();
        jdbc.update("delete from violation where inspection_id in (:ids)",
                new MapSqlParameterSource("ids", ids));

        List<SqlParameterSource> violationRows = new ArrayList<>();
        for (InspectionRecord record : records) {
            List<ViolationLine> lines = ViolationParser.parse(record.violations());
            for (int i = 0; i < lines.size(); i++) {
                ViolationLine line = lines.get(i);
                violationRows.add(new MapSqlParameterSource()
                        .addValue("inspectionId", record.inspectionId())
                        .addValue("ordinal", i)
                        .addValue("code", line.code())
                        .addValue("description", line.description())
                        .addValue("comment", line.comment()));
            }
        }
        if (!violationRows.isEmpty()) {
            jdbc.batchUpdate("""
                            insert into violation (inspection_id, ordinal, code, description, comment)
                            values (:inspectionId, :ordinal, :code, :description, :comment)
                            """,
                    violationRows.toArray(SqlParameterSource[]::new));
        }
    }
}

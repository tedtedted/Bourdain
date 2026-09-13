package com.tedredington.bourdain.inspection.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.InspectionType;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;

class InspectionRepositoryCustomImpl implements InspectionRepositoryCustom {

    /**
     * Archives the stored row if the incoming one differs, then upserts. One
     * statement per row: both parts see the row as it was before this statement,
     * and each batch row sees the effect of the one before it.
     */
    private static final String UPSERT = """
            with amended as (
                insert into inspection_revision (inspection_id, license_number, dba_name, inspected_on, result,
                                                 inspection_type, inspection_type_raw, violations_raw)
                select id, license_number, dba_name, inspected_on, result,
                       inspection_type, inspection_type_raw, violations_raw
                from inspection
                where id = :id
                  and (license_number, dba_name, inspected_on, result, inspection_type_raw, violations_raw)
                      is distinct from (:licenseNumber, :dbaName, :inspectedOn, :result, :typeRaw, :violationsRaw)
            )
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

    private final JdbcClient jdbc;
    private final NamedParameterJdbcTemplate batch;

    InspectionRepositoryCustomImpl(JdbcClient jdbc, NamedParameterJdbcTemplate batch) {
        this.jdbc = jdbc;
        this.batch = batch;
    }

    @Override
    public void upsertAll(List<Inspection> inspections) {
        batch.batchUpdate(UPSERT, inspections.stream()
                .map(i -> new MapSqlParameterSource()
                        .addValue("id", i.id())
                        .addValue("licenseNumber", i.licenseNumber())
                        .addValue("dbaName", i.dbaName())
                        .addValue("inspectedOn", i.inspectedOn())
                        .addValue("result", i.result().name())
                        .addValue("type", i.type().name())
                        .addValue("typeRaw", i.typeRaw())
                        .addValue("violationsRaw", i.violationsRaw()))
                .toArray(SqlParameterSource[]::new));

        // Violations are derived from violations_raw, so delete + insert is the
        // simplest idempotent shape.
        jdbc.sql("delete from violation where inspection_id in (:ids)")
                .param("ids", inspections.stream().map(Inspection::id).toList())
                .update();

        List<SqlParameterSource> violationRows = new ArrayList<>();
        for (Inspection inspection : inspections) {
            List<Violation> violations = inspection.violations();
            for (int ordinal = 0; ordinal < violations.size(); ordinal++) {
                Violation violation = violations.get(ordinal);
                violationRows.add(new MapSqlParameterSource()
                        .addValue("inspectionId", inspection.id())
                        .addValue("ordinal", ordinal)
                        .addValue("code", violation.code())
                        .addValue("description", violation.description())
                        .addValue("comment", violation.comment()));
            }
        }
        if (!violationRows.isEmpty()) {
            batch.batchUpdate("""
                            insert into violation (inspection_id, ordinal, code, description, comment)
                            values (:inspectionId, :ordinal, :code, :description, :comment)
                            """,
                    violationRows.toArray(SqlParameterSource[]::new));
        }
    }

    @Override
    public List<Inspection> findHistory(long licenseNumber) {
        record Header(long id, long licenseNumber, String dbaName, LocalDate inspectedOn, InspectionResult result,
                      InspectionType type, String typeRaw, String violationsRaw) {
        }

        List<Header> headers = jdbc.sql("""
                        select id, license_number, dba_name, inspected_on, result,
                               inspection_type, inspection_type_raw, violations_raw
                        from inspection
                        where license_number = :license
                        order by inspected_on desc, id desc
                        """)
                .param("license", licenseNumber)
                .query((rs, n) -> new Header(
                        rs.getLong("id"),
                        rs.getLong("license_number"),
                        rs.getString("dba_name"),
                        rs.getObject("inspected_on", LocalDate.class),
                        InspectionResult.valueOf(rs.getString("result")),
                        InspectionType.valueOf(rs.getString("inspection_type")),
                        rs.getString("inspection_type_raw"),
                        rs.getString("violations_raw")))
                .list();
        if (headers.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Violation>> violations = jdbc.sql("""
                        select inspection_id, code, description, comment
                        from violation
                        where inspection_id in (:ids)
                        order by inspection_id, ordinal
                        """)
                .param("ids", headers.stream().map(Header::id).toList())
                .query((rs, n) -> Map.entry(
                        rs.getLong("inspection_id"),
                        new Violation(rs.getInt("code"), rs.getString("description"), rs.getString("comment"))))
                .list().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        return headers.stream()
                .map(h -> new Inspection(h.id(), h.licenseNumber(), h.dbaName(), h.inspectedOn(), h.result(),
                        h.type(), h.typeRaw(), h.violationsRaw(), violations.getOrDefault(h.id(), List.of())))
                .toList();
    }
}

package com.tedredington.bourdain.inspection.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.InspectionType;
import com.tedredington.bourdain.inspection.Inspections.InspectionDetail;
import com.tedredington.bourdain.inspection.Inspections.RecentFailure;
import com.tedredington.bourdain.inspection.Inspections.ViolationLine;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class InspectionRepository {

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

    InspectionRepository(JdbcClient jdbc, NamedParameterJdbcTemplate batch) {
        this.jdbc = jdbc;
        this.batch = batch;
    }

    /**
     * Idempotent: re-saving an inspection upserts the same row and rebuilds its
     * violations. The version an amended inspection replaces is kept as a revision.
     */
    void saveAll(List<Inspection> inspections) {
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
            List<ViolationLine> lines = inspection.violations();
            for (int ordinal = 0; ordinal < lines.size(); ordinal++) {
                ViolationLine line = lines.get(ordinal);
                violationRows.add(new MapSqlParameterSource()
                        .addValue("inspectionId", inspection.id())
                        .addValue("ordinal", ordinal)
                        .addValue("code", line.code())
                        .addValue("description", line.description())
                        .addValue("comment", line.comment()));
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

    List<RecentFailure> findRecentFailures(int limit) {
        return jdbc.sql("""
                        select i.id as inspection_id, i.license_number, i.dba_name, i.inspected_on,
                               (select v.description from violation v
                                where v.inspection_id = i.id order by v.ordinal limit 1) as headline,
                               (select count(*) from violation v where v.inspection_id = i.id) as violation_count
                        from inspection i
                        where i.result = 'FAIL'
                        order by i.inspected_on desc, i.id desc
                        limit :limit
                        """)
                .param("limit", limit)
                .query(RecentFailure.class)
                .list();
    }

    /** Newest first, violations in the order the inspector listed them. */
    List<InspectionDetail> findHistory(long licenseNumber) {
        record Row(long id, LocalDate inspectedOn, InspectionType type, String typeRaw, InspectionResult result) {
        }

        List<Row> rows = jdbc.sql("""
                        select id, inspected_on, inspection_type, inspection_type_raw, result
                        from inspection
                        where license_number = :license
                        order by inspected_on desc, id desc
                        """)
                .param("license", licenseNumber)
                .query((rs, n) -> new Row(
                        rs.getLong("id"),
                        rs.getObject("inspected_on", LocalDate.class),
                        InspectionType.valueOf(rs.getString("inspection_type")),
                        rs.getString("inspection_type_raw"),
                        InspectionResult.valueOf(rs.getString("result"))))
                .list();
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ViolationLine>> violations = jdbc.sql("""
                        select inspection_id, code, description, comment
                        from violation
                        where inspection_id in (:ids)
                        order by inspection_id, ordinal
                        """)
                .param("ids", rows.stream().map(Row::id).toList())
                .query((rs, n) -> Map.entry(
                        rs.getLong("inspection_id"),
                        new ViolationLine(rs.getInt("code"), rs.getString("description"), rs.getString("comment"))))
                .list().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        return rows.stream()
                .map(row -> new InspectionDetail(row.id(), row.inspectedOn(), row.type(), row.typeRaw(),
                        row.result(), violations.getOrDefault(row.id(), List.of())))
                .toList();
    }
}

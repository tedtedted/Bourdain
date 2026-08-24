package com.tedredington.bourdain.inspection.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.InspectionType;
import com.tedredington.bourdain.inspection.Inspections;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
class InspectionsImpl implements Inspections {

    private final JdbcClient jdbc;

    InspectionsImpl(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RecentFailure> recentFailures(int limit) {
        return jdbc.sql("""
                        select i.id, i.license_number, i.dba_name, i.inspected_on,
                               (select v.description from violation v
                                where v.inspection_id = i.id order by v.ordinal limit 1) as headline,
                               (select count(*) from violation v where v.inspection_id = i.id) as violation_count
                        from inspection i
                        where i.result = 'FAIL'
                        order by i.inspected_on desc, i.id desc
                        limit :limit
                        """)
                .param("limit", limit)
                .query((rs, n) -> new RecentFailure(
                        rs.getLong("id"),
                        rs.getLong("license_number"),
                        rs.getString("dba_name"),
                        rs.getDate("inspected_on").toLocalDate(),
                        rs.getString("headline"),
                        rs.getInt("violation_count")))
                .list();
    }

    @Override
    public List<InspectionDetail> history(long licenseNumber) {
        record Row(long id, java.time.LocalDate inspectedOn, InspectionType type, String typeRaw,
                   InspectionResult result) {
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
                        rs.getDate("inspected_on").toLocalDate(),
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

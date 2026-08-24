package com.tedredington.bourdain.establishment.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tedredington.bourdain.civicdata.CivicDataSyncCompleted;
import com.tedredington.bourdain.establishment.EstablishmentView.Relocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/**
 * The sync → matching handoff. Runs after a sync run's transaction commits
 * (Modulith registry event, so a crash here is retried on restart) and
 * recomputes every establishment's status from scratch — the derivation is
 * cheap and idempotence beats bookkeeping about what changed.
 */
@Service
class RelocationService {

    private static final Logger log = LoggerFactory.getLogger(RelocationService.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    RelocationService(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @ApplicationModuleListener
    void on(CivicDataSyncCompleted event) {
        log.info("Deriving establishment statuses after {} sync", event.source());
        deriveStatuses();
    }

    void deriveStatuses() {
        // Reset to the two base states; matching below re-applies RELOCATED.
        jdbc.update("""
                update establishment
                set status = case when latest_result = 'OUT_OF_BUSINESS' then 'CLOSED' else 'OPEN' end,
                    relocated_to_license_number = null, relocated_to_address = null, relocated_since = null
                """, new MapSqlParameterSource());

        Map<Long, RelocationMatcher.Closed> closed = new LinkedHashMap<>();
        Map<Long, List<RelocationMatcher.Candidate>> candidates = new LinkedHashMap<>();
        jdbc.query("""
                        select e.license_number as closed_license, e.address as closed_address, e.last_inspected_on,
                               bl.license_number as candidate_license, bl.address as candidate_address,
                               bl.license_start_date, bl.expiration_date
                        from establishment e
                        join business_license bl on bl.normalized_name = e.normalized_name
                        where e.status = 'CLOSED' and bl.license_number <> e.license_number
                        """,
                rs -> {
                    long closedLicense = rs.getLong("closed_license");
                    RelocationMatcher.Closed closedRow = new RelocationMatcher.Closed(
                            closedLicense,
                            rs.getString("closed_address"),
                            localDate(rs.getObject("last_inspected_on", java.sql.Date.class)));
                    RelocationMatcher.Candidate candidateRow = new RelocationMatcher.Candidate(
                            rs.getLong("candidate_license"),
                            rs.getString("candidate_address"),
                            localDate(rs.getObject("license_start_date", java.sql.Date.class)),
                            localDate(rs.getObject("expiration_date", java.sql.Date.class)));
                    closed.putIfAbsent(closedLicense, closedRow);
                    candidates.computeIfAbsent(closedLicense, key -> new ArrayList<>()).add(candidateRow);
                });

        LocalDate today = LocalDate.now(clock);
        List<SqlParameterSource> updates = new ArrayList<>();
        for (RelocationMatcher.Closed c : closed.values()) {
            RelocationMatcher.match(c, candidates.get(c.licenseNumber()), today)
                    .ifPresent(relocation -> updates.add(params(c.licenseNumber(), relocation)));
        }
        if (!updates.isEmpty()) {
            jdbc.batchUpdate("""
                            update establishment
                            set status = 'RELOCATED', relocated_to_license_number = :toLicense,
                                relocated_to_address = :toAddress, relocated_since = :since, updated_at = now()
                            where license_number = :license
                            """,
                    updates.toArray(SqlParameterSource[]::new));
        }
        log.info("Status derivation done: {} closed establishments, {} marked relocated", closed.size(), updates.size());
    }

    private static SqlParameterSource params(long licenseNumber, Relocation relocation) {
        return new MapSqlParameterSource()
                .addValue("license", licenseNumber)
                .addValue("toLicense", relocation.licenseNumber())
                .addValue("toAddress", relocation.address())
                .addValue("since", relocation.since());
    }

    private static LocalDate localDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }
}

package com.tedredington.bourdain.establishment.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tedredington.bourdain.establishment.EstablishmentView.Relocation;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

class EstablishmentRepositoryCustomImpl implements EstablishmentRepositoryCustom {

    /**
     * Archives the stored details if a not-older inspection changes them, then
     * upserts under the same condition. One statement per row, so a page with
     * several newer inspections for one license archives each version once.
     */
    private static final String UPSERT_LATEST = """
            with changed as (
                insert into establishment_revision (license_number, name, aka_name, facility_type_raw,
                                                    facility_category, risk, address, city, state, zip,
                                                    latitude, longitude, last_inspected_on)
                select license_number, name, aka_name, facility_type_raw,
                       facility_category, risk, address, city, state, zip,
                       latitude, longitude, last_inspected_on
                from establishment
                where license_number = :licenseNumber
                  and :lastInspectedOn >= coalesce(last_inspected_on, date '1900-01-01')
                  and (name, aka_name, facility_type_raw, risk, address, city, state, zip, latitude, longitude)
                      is distinct from (:name, :akaName, :facilityTypeRaw, :risk, :address, :city, :state, :zip,
                                        :latitude, :longitude)
            )
            insert into establishment (license_number, name, normalized_name, aka_name, facility_type_raw,
                                       facility_category, risk, address, city, state, zip, latitude, longitude,
                                       latest_result, last_inspected_on)
            values (:licenseNumber, :name, :normalizedName, :akaName, :facilityTypeRaw, :facilityCategory,
                    :risk, :address, :city, :state, :zip, :latitude, :longitude, :latestResult, :lastInspectedOn)
            on conflict (license_number) do update set
                name = excluded.name,
                normalized_name = excluded.normalized_name,
                aka_name = excluded.aka_name,
                facility_type_raw = excluded.facility_type_raw,
                facility_category = excluded.facility_category,
                risk = excluded.risk,
                address = excluded.address,
                city = excluded.city,
                state = excluded.state,
                zip = excluded.zip,
                latitude = excluded.latitude,
                longitude = excluded.longitude,
                latest_result = excluded.latest_result,
                last_inspected_on = excluded.last_inspected_on,
                updated_at = now()
            where excluded.last_inspected_on >= coalesce(establishment.last_inspected_on, date '1900-01-01')
            """;

    private final NamedParameterJdbcTemplate jdbc;

    EstablishmentRepositoryCustomImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsertLatest(List<EstablishmentSnapshot> snapshots) {
        jdbc.batchUpdate(UPSERT_LATEST, snapshots.stream()
                .map(s -> new MapSqlParameterSource()
                        .addValue("licenseNumber", s.licenseNumber())
                        .addValue("name", s.name())
                        .addValue("normalizedName", s.normalizedName())
                        .addValue("akaName", s.akaName())
                        .addValue("facilityTypeRaw", s.facilityTypeRaw())
                        .addValue("facilityCategory", s.facilityCategory().name())
                        .addValue("risk", s.risk().name())
                        .addValue("address", s.address())
                        .addValue("city", s.city())
                        .addValue("state", s.state())
                        .addValue("zip", s.zip())
                        .addValue("latitude", s.latitude())
                        .addValue("longitude", s.longitude())
                        .addValue("latestResult", s.latestResult().name())
                        .addValue("lastInspectedOn", s.lastInspectedOn()))
                .toArray(SqlParameterSource[]::new));
    }

    @Override
    public void resetStatuses() {
        jdbc.update("""
                update establishment
                set status = case when latest_result = 'OUT_OF_BUSINESS' then 'CLOSED' else 'OPEN' end,
                    relocated_to_license_number = null, relocated_to_address = null, relocated_since = null
                """, new MapSqlParameterSource());
    }

    @Override
    public Map<RelocationMatcher.Closed, List<RelocationMatcher.Candidate>> findClosedWithNamesakes() {
        Map<RelocationMatcher.Closed, List<RelocationMatcher.Candidate>> closed = new LinkedHashMap<>();
        jdbc.query("""
                        select e.license_number as closed_license, e.address as closed_address, e.last_inspected_on,
                               bl.license_number as candidate_license, bl.address as candidate_address,
                               bl.license_start_date, bl.expiration_date
                        from establishment e
                        join business_license bl on bl.normalized_name = e.normalized_name
                        where e.status = 'CLOSED' and bl.license_number <> e.license_number
                          and bl.delisted_at is null
                        """,
                rs -> {
                    var establishment = new RelocationMatcher.Closed(
                            rs.getLong("closed_license"),
                            rs.getString("closed_address"),
                            rs.getObject("last_inspected_on", LocalDate.class));
                    var candidate = new RelocationMatcher.Candidate(
                            rs.getLong("candidate_license"),
                            rs.getString("candidate_address"),
                            rs.getObject("license_start_date", LocalDate.class),
                            rs.getObject("expiration_date", LocalDate.class));
                    closed.computeIfAbsent(establishment, key -> new ArrayList<>()).add(candidate);
                });
        return closed;
    }

    @Override
    public void markRelocated(Map<Long, Relocation> relocations) {
        if (relocations.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                        update establishment
                        set status = 'RELOCATED', relocated_to_license_number = :toLicense,
                            relocated_to_address = :toAddress, relocated_since = :since, updated_at = now()
                        where license_number = :license
                        """,
                relocations.entrySet().stream()
                        .map(entry -> new MapSqlParameterSource()
                                .addValue("license", entry.getKey())
                                .addValue("toLicense", entry.getValue().licenseNumber())
                                .addValue("toAddress", entry.getValue().address())
                                .addValue("since", entry.getValue().since()))
                        .toArray(SqlParameterSource[]::new));
    }

    @Override
    public int recordStatusChanges() {
        return jdbc.update("""
                insert into establishment_status_change (license_number, status, relocated_to_license_number,
                                                         relocated_to_address, relocated_since)
                select e.license_number, e.status, e.relocated_to_license_number,
                       e.relocated_to_address, e.relocated_since
                from establishment e
                left join lateral (
                    select c.status, c.relocated_to_license_number
                    from establishment_status_change c
                    where c.license_number = e.license_number
                    order by c.id desc
                    limit 1
                ) last on true
                where (e.status, e.relocated_to_license_number)
                      is distinct from (last.status, last.relocated_to_license_number)
                """, new MapSqlParameterSource());
    }
}

package com.tedredington.bourdain.establishment.internal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SimplePropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
class BusinessLicenseRepository {

    private static final String UPSERT = """
            insert into business_license (record_id, license_number, dba_name, normalized_name, legal_name,
                                          license_description, address, city, state, zip,
                                          license_start_date, expiration_date, status_raw, latitude, longitude)
            values (:recordId, :licenseNumber, :dbaName, :normalizedName, :legalName, :licenseDescription,
                    :address, :city, :state, :zip, :licenseStartDate, :expirationDate, :statusRaw,
                    :latitude, :longitude)
            on conflict (record_id) do update set
                license_number = excluded.license_number,
                dba_name = excluded.dba_name,
                normalized_name = excluded.normalized_name,
                legal_name = excluded.legal_name,
                license_description = excluded.license_description,
                address = excluded.address,
                city = excluded.city,
                state = excluded.state,
                zip = excluded.zip,
                license_start_date = excluded.license_start_date,
                expiration_date = excluded.expiration_date,
                status_raw = excluded.status_raw,
                latitude = excluded.latitude,
                longitude = excluded.longitude,
                delisted_at = null,
                updated_at = now()
            """;

    private final NamedParameterJdbcTemplate jdbc;

    BusinessLicenseRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Upserting a license also lists it again if it had been delisted. */
    void upsertAll(List<BusinessLicense> licenses) {
        jdbc.batchUpdate(UPSERT, licenses.stream()
                .map(SimplePropertySqlParameterSource::new)
                .toArray(SqlParameterSource[]::new));
    }

    /**
     * Marks every listed license not upserted since {@code cutoff}. Rows are
     * stamped with the database's {@code now()}, so the cutoff must come from
     * the same clock, not the JVM's; the two can disagree. Marked rather than
     * deleted: once the city drops a license, this mirror is the only record
     * it existed.
     */
    int delistNotUpsertedSince(Instant cutoff) {
        return jdbc.update("""
                        update business_license set delisted_at = now()
                        where updated_at < :cutoff and delisted_at is null
                        """,
                new MapSqlParameterSource("cutoff", cutoff.atOffset(ZoneOffset.UTC)));
    }
}

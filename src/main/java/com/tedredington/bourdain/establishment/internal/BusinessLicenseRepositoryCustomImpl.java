package com.tedredington.bourdain.establishment.internal;

import java.util.List;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SimplePropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

class BusinessLicenseRepositoryCustomImpl implements BusinessLicenseRepositoryCustom {

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

    BusinessLicenseRepositoryCustomImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsertAll(List<BusinessLicense> licenses) {
        jdbc.batchUpdate(UPSERT, licenses.stream()
                .map(SimplePropertySqlParameterSource::new)
                .toArray(SqlParameterSource[]::new));
    }
}

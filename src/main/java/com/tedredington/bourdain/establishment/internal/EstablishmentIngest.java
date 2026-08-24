package com.tedredington.bourdain.establishment.internal;

import com.tedredington.bourdain.civicdata.InspectionBatchReceived;
import com.tedredington.bourdain.civicdata.LicenseBatchReceived;
import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.InspectionResult;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * Maintains the establishment row per license from the inspection stream, and
 * mirrors active business licenses. An establishment reflects its most recent
 * inspection: the conditional upsert ignores rows older than what's stored, so
 * batch order within a page doesn't matter.
 */
@Component
class EstablishmentIngest {

    private static final String UPSERT_ESTABLISHMENT = """
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

    private static final String UPSERT_LICENSE = """
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
                updated_at = now()
            """;

    private final NamedParameterJdbcTemplate jdbc;

    EstablishmentIngest(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener
    void on(InspectionBatchReceived batch) {
        SqlParameterSource[] rows = batch.records().stream()
                .map(r -> new MapSqlParameterSource()
                        .addValue("licenseNumber", r.licenseNumber())
                        .addValue("name", r.dbaName())
                        .addValue("normalizedName", NameNormalizer.normalize(r.dbaName()))
                        .addValue("akaName", r.akaName())
                        .addValue("facilityTypeRaw", r.facilityType())
                        .addValue("facilityCategory", FacilityCategory.classify(r.facilityType()).name())
                        .addValue("risk", Risk.fromRaw(r.risk()).name())
                        .addValue("address", r.address() == null ? "" : r.address())
                        .addValue("city", r.city())
                        .addValue("state", r.state())
                        .addValue("zip", r.zip())
                        .addValue("latitude", r.latitude())
                        .addValue("longitude", r.longitude())
                        .addValue("latestResult", InspectionResult.fromRaw(r.result()).name())
                        .addValue("lastInspectedOn", r.inspectedOn()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(UPSERT_ESTABLISHMENT, rows);
    }

    @EventListener
    void on(LicenseBatchReceived batch) {
        SqlParameterSource[] rows = batch.records().stream()
                .map(r -> new MapSqlParameterSource()
                        .addValue("recordId", r.recordId())
                        .addValue("licenseNumber", r.licenseNumber())
                        .addValue("dbaName", r.dbaName())
                        .addValue("normalizedName", NameNormalizer.normalize(r.dbaName()))
                        .addValue("legalName", r.legalName())
                        .addValue("licenseDescription", r.licenseDescription())
                        .addValue("address", r.address())
                        .addValue("city", r.city())
                        .addValue("state", r.state())
                        .addValue("zip", r.zip())
                        .addValue("licenseStartDate", r.licenseStartDate())
                        .addValue("expirationDate", r.expirationDate())
                        .addValue("statusRaw", r.statusRaw())
                        .addValue("latitude", r.latitude())
                        .addValue("longitude", r.longitude()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(UPSERT_LICENSE, rows);
    }
}

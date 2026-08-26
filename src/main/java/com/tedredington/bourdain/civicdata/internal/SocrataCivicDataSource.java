package com.tedredington.bourdain.civicdata.internal;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tedredington.bourdain.civicdata.InspectionRecord;
import com.tedredington.bourdain.civicdata.LicenseRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * SODA 2.1 client for data.cityofchicago.org. Uses keyset pagination on the
 * Socrata system row id ({@code :id}) because large {@code $offset} scans
 * degrade, and {@code :updated_at} as the incremental watermark.
 */
@Component
@EnableConfigurationProperties({SocrataProperties.class, SyncProperties.class})
class SocrataCivicDataSource implements CivicDataSource {

    private static final Logger log = LoggerFactory.getLogger(SocrataCivicDataSource.class);

    private static final ParameterizedTypeReference<List<Map<String, Object>>> ROWS =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final SocrataProperties properties;

    @Autowired
    SocrataCivicDataSource(RestClient.Builder builder, SocrataProperties properties) {
        this(configuredRestClient(builder, properties), properties);
    }

    SocrataCivicDataSource(RestClient restClient, SocrataProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    private static RestClient configuredRestClient(RestClient.Builder builder, SocrataProperties properties) {
        RestClient.Builder configured = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory(properties.connectTimeout(), properties.readTimeout()));
        if (properties.hasAppToken()) {
            configured = configured.defaultHeader("X-App-Token", properties.appToken());
        }
        return configured.build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    @Override
    public InspectionPage inspectionsPage(String updatedSince, String lastRowId, int pageSize) {
        List<String> conditions = new ArrayList<>();
        conditions.add("license_ > 0"); // rows without a real license are unusable noise
        if (updatedSince != null) {
            conditions.add(":updated_at > '" + updatedSince + "'");
        }
        if (lastRowId != null) {
            conditions.add(":id > '" + lastRowId + "'");
        }

        List<Map<String, Object>> rows = fetch(properties.inspectionsDataset(), conditions, pageSize);

        List<InspectionRecord> records = new ArrayList<>(rows.size());
        String maxUpdatedAt = null;
        String newLastRowId = null;
        int skipped = 0;
        for (Map<String, Object> row : rows) {
            newLastRowId = str(row, ":id");
            String updatedAt = str(row, ":updated_at");
            if (maxUpdatedAt == null || (updatedAt != null && updatedAt.compareTo(maxUpdatedAt) > 0)) {
                maxUpdatedAt = updatedAt;
            }
            InspectionRecord record = toInspection(row);
            if (record == null) {
                skipped++;
            } else {
                records.add(record);
            }
        }
        return new InspectionPage(records, newLastRowId, maxUpdatedAt, skipped);
    }

    @Override
    public LicensePage licensesPage(String lastRowId, int pageSize) {
        String descriptions = properties.licenseDescriptions().stream()
                .map(d -> "'" + d.replace("'", "''") + "'")
                .collect(Collectors.joining(","));

        List<String> conditions = new ArrayList<>();
        conditions.add("license_status = 'AAI'");
        conditions.add("license_description in (" + descriptions + ")");
        if (lastRowId != null) {
            conditions.add(":id > '" + lastRowId + "'");
        }

        List<Map<String, Object>> rows = fetch(properties.licensesDataset(), conditions, pageSize);

        List<LicenseRecord> records = new ArrayList<>(rows.size());
        String newLastRowId = null;
        int skipped = 0;
        for (Map<String, Object> row : rows) {
            newLastRowId = str(row, ":id");
            LicenseRecord record = toLicense(row);
            if (record == null) {
                skipped++;
            } else {
                records.add(record);
            }
        }
        return new LicensePage(records, newLastRowId, skipped);
    }

    private List<Map<String, Object>> fetch(String dataset, List<String> conditions, int pageSize) {
        String where = String.join(" AND ", conditions);
        try {
            List<Map<String, Object>> rows = restClient.get()
                    .uri(builder -> uri(builder, dataset, where, pageSize))
                    .retrieve()
                    .body(ROWS);
            return rows == null ? List.of() : rows;
        } catch (RestClientException e) {
            throw new CivicDataSourceException("Failed to fetch Socrata dataset " + dataset, e);
        }
    }

    private java.net.URI uri(UriBuilder builder, String dataset, String where, int pageSize) {
        return builder.path("/resource/{dataset}.json")
                .queryParam("$select", "*,:id,:updated_at")
                .queryParam("$where", where)
                .queryParam("$order", ":id")
                .queryParam("$limit", pageSize)
                .build(dataset);
    }

    private InspectionRecord toInspection(Map<String, Object> row) {
        Long inspectionId = longOrNull(row, "inspection_id");
        Long licenseNumber = longOrNull(row, "license_");
        LocalDate inspectedOn = dateOrNull(row, "inspection_date");
        String dbaName = str(row, "dba_name");
        if (inspectionId == null || licenseNumber == null || licenseNumber <= 0
                || inspectedOn == null || dbaName == null || dbaName.isBlank()) {
            log.debug("Skipping unusable inspection row {}", row.get(":id"));
            return null;
        }
        return new InspectionRecord(
                inspectionId,
                licenseNumber,
                dbaName.trim(),
                str(row, "aka_name"),
                str(row, "facility_type"),
                str(row, "risk"),
                trimmed(row, "address"),
                str(row, "city"),
                str(row, "state"),
                str(row, "zip"),
                inspectedOn,
                str(row, "inspection_type"),
                str(row, "results"),
                str(row, "violations"),
                doubleOrNull(row, "latitude"),
                doubleOrNull(row, "longitude"));
    }

    private LicenseRecord toLicense(Map<String, Object> row) {
        String recordId = str(row, "id");
        Long licenseNumber = longOrNull(row, "license_number");
        String description = str(row, "license_description");
        String address = trimmed(row, "address");
        if (recordId == null || licenseNumber == null || description == null || address == null) {
            log.debug("Skipping unusable license row {}", row.get(":id"));
            return null;
        }
        return new LicenseRecord(
                recordId,
                licenseNumber,
                str(row, "doing_business_as_name"),
                str(row, "legal_name"),
                description,
                address,
                str(row, "city"),
                str(row, "state"),
                str(row, "zip_code"),
                dateOrNull(row, "license_start_date"),
                dateOrNull(row, "expiration_date"),
                str(row, "license_status"),
                doubleOrNull(row, "latitude"),
                doubleOrNull(row, "longitude"));
    }

    // Socrata serializes numbers as JSON strings; all field access funnels
    // through these lenient converters.

    private static String str(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : value.toString();
    }

    private static String trimmed(Map<String, Object> row, String key) {
        String value = str(row, key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Long longOrNull(Map<String, Object> row, String key) {
        String value = str(row, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return (long) Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double doubleOrNull(Map<String, Object> row, String key) {
        String value = str(row, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate dateOrNull(Map<String, Object> row, String key) {
        String value = str(row, key);
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }
}

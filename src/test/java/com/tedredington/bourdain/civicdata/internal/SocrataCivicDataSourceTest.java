package com.tedredington.bourdain.civicdata.internal;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import com.tedredington.bourdain.civicdata.InspectionRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SocrataCivicDataSourceTest {

    private MockRestServiceServer server;
    private SocrataCivicDataSource source;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        SocrataProperties properties = new SocrataProperties(
                "https://data.example.org", "test-token", 1000, Duration.ofSeconds(5), Duration.ofSeconds(30),
                "insp-ds", "lic-ds",
                List.of("Retail Food Establishment", "Tavern"));
        source = new SocrataCivicDataSource(builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-App-Token", properties.appToken())
                .build(), properties);
    }

    /** Asserts against the decoded query string, which carries the SoQL clauses. */
    private static RequestMatcher query(String path, String... expectedInQuery) {
        return request -> {
            assertThat(request.getURI().getPath()).isEqualTo(path);
            assertThat(request.getURI().getQuery()).contains(expectedInQuery);
        };
    }

    @Test
    void mapsRowsAndTracksKeysetCursorAndWatermark() {
        server.expect(query("/resource/insp-ds.json",
                        "license_ > 0",
                        ":updated_at > '2026-01-01T00:00:00.000'",
                        "$order=:id"))
                .andExpect(header("X-App-Token", "test-token"))
                .andRespond(withSuccess("""
                        [{
                          ":id": "row-aaa", ":updated_at": "2026-02-01T05:00:00.000",
                          "inspection_id": "2597589", "dba_name": "THE DUKE OF PERTH",
                          "aka_name": "THE DUKE OF PERTH", "license_": "18158",
                          "facility_type": "Restaurant", "risk": "Risk 1 (High)",
                          "address": "2913 N CLARK ST ", "city": "CHICAGO", "state": "IL", "zip": "60657",
                          "inspection_date": "2024-07-16T00:00:00.000", "inspection_type": "Canvass",
                          "results": "Out of Business", "latitude": "41.934", "longitude": "-87.644"
                        },
                        {
                          ":id": "row-bbb", ":updated_at": "2026-02-02T05:00:00.000",
                          "inspection_id": "9", "dba_name": "MISSING BITS"
                        }]
                        """, MediaType.APPLICATION_JSON));

        var page = source.inspectionsPage("2026-01-01T00:00:00.000", null, 1000);

        assertThat(page.records()).hasSize(1);
        InspectionRecord record = page.records().getFirst();
        assertThat(record.inspectionId()).isEqualTo(2597589L);
        assertThat(record.licenseNumber()).isEqualTo(18158L);
        assertThat(record.inspectedOn()).isEqualTo(LocalDate.of(2024, 7, 16));
        assertThat(record.address()).isEqualTo("2913 N CLARK ST");

        // The row without license/date is skipped but still advances the cursor.
        assertThat(page.skipped()).isEqualTo(1);
        assertThat(page.lastRowId()).isEqualTo("row-bbb");
        assertThat(page.maxUpdatedAt()).isEqualTo("2026-02-02T05:00:00.000");
    }

    @Test
    void emptyResponseEndsTheScan() {
        server.expect(query("/resource/insp-ds.json", ":id > 'row-zzz'"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var page = source.inspectionsPage(null, "row-zzz", 1000);

        assertThat(page.records()).isEmpty();
        assertThat(page.lastRowId()).isNull();
    }

    @Test
    void licensesQueryFiltersToActiveFoodLicenses() {
        server.expect(query("/resource/lic-ds.json",
                        "license_status = 'AAI'",
                        "license_description in ('Retail Food Establishment','Tavern')"))
                .andRespond(withSuccess("""
                        [{
                          ":id": "row-ccc",
                          "id": "2252465-20250216", "license_number": "2252465",
                          "legal_name": "SHORTY O'TOOLE'S PUB, INC.", "doing_business_as_name": "Duke of Perth",
                          "license_description": "Tavern", "address": "2827 N BROADWAY  1",
                          "city": "CHICAGO", "state": "IL", "zip_code": "60657",
                          "license_start_date": "2025-02-16T00:00:00.000",
                          "expiration_date": "2027-02-15T00:00:00.000", "license_status": "AAI"
                        }]
                        """, MediaType.APPLICATION_JSON));

        var page = source.licensesPage(null, 1000);

        assertThat(page.records()).hasSize(1);
        assertThat(page.records().getFirst().licenseNumber()).isEqualTo(2252465L);
        assertThat(page.records().getFirst().licenseStartDate()).isEqualTo(LocalDate.of(2025, 2, 16));
    }

    @Test
    void wrapsUpstreamErrorsWithDatasetContext() {
        server.expect(query("/resource/insp-ds.json", "license_ > 0"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> source.inspectionsPage(null, null, 1000))
                .isInstanceOf(CivicDataSourceException.class)
                .hasMessageContaining("insp-ds")
                .hasCauseInstanceOf(RestClientException.class);
    }
}

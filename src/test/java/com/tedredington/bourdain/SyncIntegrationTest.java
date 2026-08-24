package com.tedredington.bourdain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.tedredington.bourdain.civicdata.InspectionRecord;
import com.tedredington.bourdain.civicdata.LicenseRecord;
import com.tedredington.bourdain.civicdata.SyncSource;
import com.tedredington.bourdain.civicdata.SyncStatus;
import com.tedredington.bourdain.civicdata.internal.CivicDataSource;
import com.tedredington.bourdain.civicdata.internal.SyncService;
import com.tedredington.bourdain.establishment.EstablishmentStatus;
import com.tedredington.bourdain.establishment.Establishments;
import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.Inspections;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end sync against a real Postgres: canned Socrata pages replay the
 * actual Duke of Perth timeline (closed on Clark St, re-licensed on Broadway)
 * and must come out the other side as a RELOCATED establishment.
 */
// The short Hikari timeout keeps JVM shutdown fast: Modulith's event registry
// checks for incomplete publications on destroy, after the container is gone.
@SpringBootTest(properties = {
        "bourdain.sync.on-startup=false",
        "spring.datasource.hikari.connection-timeout=1000"
})
@Testcontainers
@Import(SyncIntegrationTest.Fixtures.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SyncIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private SyncService syncService;
    @Autowired
    private Establishments establishments;
    @Autowired
    private Inspections inspections;
    @Autowired
    private SyncStatus syncStatus;

    @Test
    @Order(1)
    void fullSyncIngestsDerivesAndMatches() {
        syncService.syncAll();

        assertThat(establishments.count()).isEqualTo(2);
        assertThat(syncStatus.lastSuccessful(SyncSource.INSPECTIONS))
                .hasValueSatisfying(sync -> assertThat(sync.rowsUpserted()).isEqualTo(3));

        // Status derivation runs asynchronously after the sync commits
        // (Modulith registry event), hence the await.
        await().atMost(15, SECONDS).untilAsserted(() -> {
            var duke = establishments.byLicenseNumber(18158).orElseThrow();
            assertThat(duke.status()).isEqualTo(EstablishmentStatus.RELOCATED);
            assertThat(duke.relocation()).isNotNull();
            assertThat(duke.relocation().licenseNumber()).isEqualTo(2252464);
            assertThat(duke.relocation().address()).isEqualTo("2827 N BROADWAY  1");
            assertThat(duke.relocation().since()).isEqualTo(LocalDate.of(2025, 2, 16));
        });

        var history = inspections.history(18158);
        assertThat(history).hasSize(2);
        assertThat(history.getFirst().result()).isEqualTo(InspectionResult.OUT_OF_BUSINESS);

        var hotDogHouse = establishments.byLicenseNumber(999).orElseThrow();
        assertThat(hotDogHouse.status()).isEqualTo(EstablishmentStatus.OPEN);
        assertThat(hotDogHouse.latestResult()).isEqualTo(InspectionResult.FAIL);

        var failures = inspections.recentFailures(10);
        assertThat(failures).hasSize(1);
        assertThat(failures.getFirst().dbaName()).isEqualTo("HOT DOG HOUSE");
        assertThat(failures.getFirst().violationCount()).isEqualTo(2);
        assertThat(failures.getFirst().headline()).startsWith("MANAGEMENT");

        assertThat(establishments.search("duke", 10))
                .anySatisfy(result -> assertThat(result.licenseNumber()).isEqualTo(18158));
    }

    @Test
    @Order(2)
    void reRunningTheSyncIsIdempotent() {
        syncService.syncAll();

        assertThat(establishments.count()).isEqualTo(2);
        assertThat(inspections.history(18158)).hasSize(2);
        await().atMost(15, SECONDS).untilAsserted(() ->
                assertThat(establishments.byLicenseNumber(18158).orElseThrow().status())
                        .isEqualTo(EstablishmentStatus.RELOCATED));
    }

    @TestConfiguration
    static class Fixtures {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneId.of("America/Chicago"));
        }

        @Bean
        @Primary
        CivicDataSource cannedCivicData() {
            List<InspectionRecord> inspectionRows = List.of(
                    new InspectionRecord(100, 18158, "THE DUKE OF PERTH", "THE DUKE OF PERTH", "Restaurant",
                            "Risk 1 (High)", "2913 N CLARK ST", "CHICAGO", "IL", "60657",
                            LocalDate.of(2023, 10, 13), "Canvass", "Pass", null, 41.934, -87.644),
                    new InspectionRecord(101, 18158, "THE DUKE OF PERTH", "THE DUKE OF PERTH", "Restaurant",
                            "Risk 1 (High)", "2913 N CLARK ST", "CHICAGO", "IL", "60657",
                            LocalDate.of(2024, 7, 16), "Canvass", "Out of Business", null, 41.934, -87.644),
                    new InspectionRecord(102, 999, "HOT DOG HOUSE", null, "Restaurant",
                            "Risk 1 (High)", "1000 W ARMITAGE AVE", "CHICAGO", "IL", "60614",
                            LocalDate.of(2026, 8, 1), "Complaint", "Fail",
                            "3. MANAGEMENT, FOOD EMPLOYEE - Comments: NO CERTIFICATE. "
                                    + "| 38. INSECTS & RODENTS - Comments: DROPPINGS OBSERVED.",
                            41.918, -87.653));

            List<LicenseRecord> licenseRows = List.of(
                    new LicenseRecord("2252464-20250216", 2252464, "Duke of Perth", "SHORTY O'TOOLE'S PUB, INC.",
                            "Retail Food Establishment", "2827 N BROADWAY  1", "CHICAGO", "IL", "60657",
                            LocalDate.of(2025, 2, 16), LocalDate.of(2027, 2, 15), "AAI", null, null));

            return new CivicDataSource() {
                @Override
                public InspectionPage inspectionsPage(String updatedSince, String lastRowId, int pageSize) {
                    return lastRowId == null
                            ? new InspectionPage(inspectionRows, "row-3", "2026-08-20T00:00:00.000", 0)
                            : new InspectionPage(List.of(), null, null, 0);
                }

                @Override
                public LicensePage licensesPage(String lastRowId, int pageSize) {
                    return lastRowId == null
                            ? new LicensePage(licenseRows, "row-1", 0)
                            : new LicensePage(List.of(), null, 0);
                }
            };
        }
    }
}

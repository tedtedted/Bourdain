package com.tedredington.bourdain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.tedredington.bourdain.civicdata.CivicDataSync;
import com.tedredington.bourdain.civicdata.InspectionRecord;
import com.tedredington.bourdain.civicdata.LicenseRecord;
import com.tedredington.bourdain.civicdata.SyncSource;
import com.tedredington.bourdain.civicdata.SyncStatus;
import com.tedredington.bourdain.civicdata.internal.CivicDataSource;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end sync against a real Postgres: canned Socrata pages replay the
 * actual Duke of Perth timeline (closed on Clark St, re-licensed on Broadway)
 * and must come out the other side as a RELOCATED establishment. Later syncs
 * change the canned data to check that what the city overwrites is kept.
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
    private CivicDataSync civicDataSync;
    @Autowired
    private Establishments establishments;
    @Autowired
    private Inspections inspections;
    @Autowired
    private SyncStatus syncStatus;
    @Autowired
    private JdbcClient jdbc;

    private static final InspectionRecord DUKE_CANVASS = new InspectionRecord(100, 18158, "THE DUKE OF PERTH",
            "THE DUKE OF PERTH", "Restaurant", "Risk 1 (High)", "2913 N CLARK ST", "CHICAGO", "IL", "60657",
            LocalDate.of(2023, 10, 13), "Canvass", "Pass", null, 41.934, -87.644);
    private static final InspectionRecord DUKE_CLOSED = new InspectionRecord(101, 18158, "THE DUKE OF PERTH",
            "THE DUKE OF PERTH", "Restaurant", "Risk 1 (High)", "2913 N CLARK ST", "CHICAGO", "IL", "60657",
            LocalDate.of(2024, 7, 16), "Canvass", "Out of Business", null, 41.934, -87.644);
    private static final InspectionRecord HOT_DOG_HOUSE_FAIL = new InspectionRecord(102, 999, "HOT DOG HOUSE", null,
            "Restaurant", "Risk 1 (High)", "1000 W ARMITAGE AVE", "CHICAGO", "IL", "60614",
            LocalDate.of(2026, 8, 1), "Complaint", "Fail",
            "3. MANAGEMENT, FOOD EMPLOYEE - Comments: NO CERTIFICATE. "
                    + "| 38. INSECTS & RODENTS - Comments: DROPPINGS OBSERVED.",
            41.918, -87.653);
    private static final LicenseRecord DUKE_BROADWAY_LICENSE = new LicenseRecord("2252464-20250216", 2252464,
            "Duke of Perth", "SHORTY O'TOOLE'S PUB, INC.", "Retail Food Establishment", "2827 N BROADWAY  1",
            "CHICAGO", "IL", "60657", LocalDate.of(2025, 2, 16), LocalDate.of(2027, 2, 15), "AAI", null, null);

    // What the canned Socrata source serves; tests swap these to simulate the city changing its data.
    private static volatile List<InspectionRecord> inspectionRows =
            List.of(DUKE_CANVASS, DUKE_CLOSED, HOT_DOG_HOUSE_FAIL);
    private static volatile List<LicenseRecord> licenseRows = List.of(DUKE_BROADWAY_LICENSE);

    @Test
    @Order(1)
    void fullSyncIngestsDerivesAndMatches() {
        civicDataSync.syncAll();

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

        assertThat(inspections.history(999).getFirst().violations())
                .extracting(violation -> violation.code())
                .containsExactly(3, 38);

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
        civicDataSync.syncAll();

        assertThat(establishments.count()).isEqualTo(2);
        assertThat(inspections.history(18158)).hasSize(2);
        await().atMost(15, SECONDS).untilAsserted(() ->
                assertThat(establishments.byLicenseNumber(18158).orElseThrow().status())
                        .isEqualTo(EstablishmentStatus.RELOCATED));

        // Re-reading unchanged data adds no history.
        assertThat(count("inspection_revision")).isZero();
        assertThat(count("establishment_revision")).isZero();
        assertThat(statusTimeline(18158)).containsExactly("RELOCATED");
        assertThat(statusTimeline(999)).containsExactly("OPEN");
    }

    @Test
    @Order(3)
    void whatTheCityOverwritesIsKept() {
        // The city amends the failed inspection, a new owner is inspected twice
        // under the reused license, and the Broadway license drops out of the feed.
        inspectionRows = List.of(DUKE_CANVASS, DUKE_CLOSED,
                new InspectionRecord(102, 999, "HOT DOG HOUSE", null, "Restaurant", "Risk 1 (High)",
                        "1000 W ARMITAGE AVE", "CHICAGO", "IL", "60614", LocalDate.of(2026, 8, 1),
                        "Complaint", "Pass", null, 41.918, -87.653),
                new InspectionRecord(103, 999, "TACO TIME", null, "Restaurant", "Risk 1 (High)",
                        "1000 W ARMITAGE AVE", "CHICAGO", "IL", "60614", LocalDate.of(2026, 8, 20),
                        "License", "Pass", null, 41.918, -87.653),
                new InspectionRecord(104, 999, "TACO TIME", null, "Restaurant", "Risk 1 (High)",
                        "1000 W ARMITAGE AVE", "CHICAGO", "IL", "60614", LocalDate.of(2026, 8, 22),
                        "Canvass", "Pass", null, 41.918, -87.653));
        licenseRows = List.of();

        civicDataSync.syncAll();

        await().atMost(15, SECONDS).untilAsserted(() ->
                assertThat(statusTimeline(18158)).containsExactly("RELOCATED", "CLOSED"));
        assertThat(establishments.byLicenseNumber(18158).orElseThrow().relocation()).isNull();
        assertThat(statusTimeline(999)).containsExactly("OPEN");

        assertThat(jdbc.sql("select delisted_at is not null from business_license where license_number = 2252464")
                .query(Boolean.class).single()).isTrue();

        assertThat(jdbc.sql("select result from inspection_revision where inspection_id = 102")
                .query(String.class).list()).containsExactly("FAIL");
        assertThat(jdbc.sql("select result from inspection where id = 102")
                .query(String.class).single()).isEqualTo("PASS");

        assertThat(jdbc.sql("select name from establishment_revision where license_number = 999")
                .query(String.class).list()).containsExactly("HOT DOG HOUSE");
        assertThat(establishments.byLicenseNumber(999).orElseThrow().name()).isEqualTo("TACO TIME");
    }

    @Test
    @Order(4)
    void aLicenseBackInTheFeedIsListedAgain() {
        licenseRows = List.of(DUKE_BROADWAY_LICENSE);

        civicDataSync.syncAll();

        await().atMost(15, SECONDS).untilAsserted(() ->
                assertThat(statusTimeline(18158)).containsExactly("RELOCATED", "CLOSED", "RELOCATED"));
        assertThat(jdbc.sql("select count(*) from business_license where delisted_at is not null")
                .query(Long.class).single()).isZero();
    }

    private long count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }

    private List<String> statusTimeline(long licenseNumber) {
        return jdbc.sql("select status from establishment_status_change where license_number = :license order by id")
                .param("license", licenseNumber)
                .query(String.class)
                .list();
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
            return new CivicDataSource() {
                @Override
                public InspectionPage inspectionsPage(String updatedSince, String lastRowId, int pageSize) {
                    return lastRowId == null
                            ? new InspectionPage(inspectionRows, "row-3", "2026-08-20T00:00:00.000Z", 0)
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

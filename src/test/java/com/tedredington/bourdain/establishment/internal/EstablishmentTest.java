package com.tedredington.bourdain.establishment.internal;

import java.time.Instant;
import java.time.LocalDate;

import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.InspectionResult;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class EstablishmentTest {

    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 8, 1);

    static EstablishmentSnapshot snapshot(long license, long inspectionId, String name, LocalDate inspectedOn) {
        return new EstablishmentSnapshot(license, inspectionId, name, NameNormalizer.normalize(name), null,
                "Restaurant", FacilityCategory.RESTAURANT, Risk.HIGH, "1000 W ARMITAGE AVE", "CHICAGO", "IL",
                "60614", 41.918, -87.653, InspectionResult.PASS, inspectedOn);
    }

    static Establishment establishment(long license, long inspectionId, String name, LocalDate inspectedOn) {
        return Establishment.firstSeen(snapshot(license, inspectionId, name, inspectedOn), NOW);
    }

    @Test
    void aLaterInspectionSupersedesAnEarlierOne() {
        Establishment establishment = establishment(999, 10, "HOT DOG HOUSE", DAY);

        assertThat(establishment.isSupersededBy(snapshot(999, 5, "TACO TIME", DAY.plusDays(1)))).isTrue();
        assertThat(establishment.isSupersededBy(snapshot(999, 50, "TACO TIME", DAY.minusDays(1)))).isFalse();
    }

    @Test
    void sameDayInspectionsSettleOnTheHigherIdSoReplaysDoNotFlipBetweenThem() {
        var lower = snapshot(999, 10, "HOT DOG HOUSE", DAY);
        var higher = snapshot(999, 11, "TACO TIME", DAY);
        Establishment establishment = Establishment.firstSeen(lower, NOW);
        establishment.apply(higher, NOW);

        assertThat(establishment.isSupersededBy(lower)).isFalse();
        assertThat(establishment.isSupersededBy(higher)).isTrue();
        assertThat(establishment.detailsDifferFrom(higher)).isFalse();
    }

    @Test
    void anEstablishmentWithoutARecordedInspectionIdFallsBackToTheDate() {
        Establishment establishment = establishment(999, 10, "HOT DOG HOUSE", DAY);
        ReflectionTestUtils.setField(establishment, "latestInspectionId", null);

        assertThat(establishment.isSupersededBy(snapshot(999, 1, "TACO TIME", DAY))).isTrue();
    }
}

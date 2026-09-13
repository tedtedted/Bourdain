package com.tedredington.bourdain.inspection.internal;

import java.time.Instant;
import java.time.LocalDate;

import com.tedredington.bourdain.civicdata.InspectionRecord;
import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.Inspections.ViolationLine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InspectionTest {

    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

    private static InspectionRecord record(String result, String violations) {
        return new InspectionRecord(102, 999, "HOT DOG HOUSE", null, "Restaurant", "Risk 1 (High)",
                "1000 W ARMITAGE AVE", "CHICAGO", "IL", "60614", LocalDate.of(2026, 8, 1), "Complaint", result,
                violations, 41.918, -87.653);
    }

    @Test
    void violationsKeepTheInspectorsOrder() {
        Inspection inspection = Inspection.published(record("Fail",
                "3. MANAGEMENT - Comments: NO CERTIFICATE. | 38. INSECTS & RODENTS - Comments: DROPPINGS."), NOW);

        assertThat(inspection.toDetail().violations()).extracting(ViolationLine::code).containsExactly(3, 38);
    }

    @Test
    void onlyAChangeInWhatTheCityPublishedIsAnAmendment() {
        Inspection inspection = Inspection.published(record("Fail", "3. MANAGEMENT - Comments: NO CERTIFICATE."), NOW);

        assertThat(inspection.isAmendedBy(record("Fail", "3. MANAGEMENT - Comments: NO CERTIFICATE."))).isFalse();
        assertThat(inspection.isAmendedBy(record("Pass", "3. MANAGEMENT - Comments: NO CERTIFICATE."))).isTrue();
    }

    @Test
    void anAmendmentRebuildsTheViolations() {
        Inspection inspection = Inspection.published(record("Fail", "3. MANAGEMENT - Comments: NO CERTIFICATE."), NOW);

        inspection.update(record("Pass", null), NOW);

        assertThat(inspection.toDetail().result()).isEqualTo(InspectionResult.PASS);
        assertThat(inspection.toDetail().violations()).isEmpty();
    }
}

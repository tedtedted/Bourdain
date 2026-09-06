package com.tedredington.bourdain.web;

import java.time.LocalDate;
import java.util.List;

import com.tedredington.bourdain.establishment.EstablishmentStatus;
import com.tedredington.bourdain.establishment.EstablishmentView;
import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.InspectionResult;
import com.tedredington.bourdain.inspection.InspectionType;
import com.tedredington.bourdain.inspection.Inspections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstablishmentPageTest {

    private static EstablishmentView view(EstablishmentStatus status, InspectionResult latest,
            LocalDate lastInspectedOn, Risk risk, EstablishmentView.Relocation relocation) {
        return new EstablishmentView(18158, "THE DUKE OF PERTH", "THE DUKE OF PERTH",
                "2913 N CLARK ST", "CHICAGO", "60657", FacilityCategory.RESTAURANT, "Restaurant",
                risk, status, latest, lastInspectedOn, relocation);
    }

    private static Inspections.InspectionDetail inspection(LocalDate on, InspectionResult result) {
        return new Inspections.InspectionDetail(1, on, InspectionType.CANVASS, "Canvass", result, List.of());
    }

    @Test
    void relocatedStandingNamesTheNewAddressAndRefusesToSayClosed() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.RELOCATED, InspectionResult.OUT_OF_BUSINESS,
                        LocalDate.of(2024, 7, 16), Risk.HIGH,
                        new EstablishmentView.Relocation(2252464, "2827 N BROADWAY", LocalDate.of(2025, 2, 16))),
                List.of());

        assertThat(page.standing().claim()).isEqualTo("Moved, not closed.");
        assertThat(page.standing().tone()).isEqualTo("relocated");
        assertThat(page.standing().evidence())
                .contains("2827 N BROADWAY")
                .contains("licensed since 16 February 2025");
    }

    @Test
    void relocationWithoutALicensedSinceDateOmitsTheClause() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.RELOCATED, InspectionResult.OUT_OF_BUSINESS,
                        LocalDate.of(2024, 7, 16), Risk.HIGH,
                        new EstablishmentView.Relocation(2252464, "2827 N BROADWAY", null)),
                List.of());

        assertThat(page.standing().evidence())
                .contains("2827 N BROADWAY")
                .doesNotContain("licensed since");
    }

    @Test
    void closedStandingShowsItsWork() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.CLOSED, InspectionResult.OUT_OF_BUSINESS,
                        LocalDate.of(2024, 7, 16), Risk.MEDIUM, null),
                List.of());

        assertThat(page.standing().claim()).isEqualTo("Last recorded out of business.");
        assertThat(page.standing().evidence())
                .contains("16 July 2024")
                .contains("No active license");
    }

    @Test
    void openStandingCitesTheLatestInspection() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.OPEN, InspectionResult.PASS_WITH_CONDITIONS,
                        LocalDate.of(2024, 3, 12), Risk.LOW, null),
                List.of());

        assertThat(page.standing().claim()).isEqualTo("Open.");
        assertThat(page.standing().evidence())
                .contains("Last inspected 12 March 2024")
                .contains("Pass w/ Conditions");
    }

    @Test
    void neverInspectedIsItsOwnStanding() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.OPEN, null, null, Risk.HIGH, null),
                List.of());

        assertThat(page.standing().claim()).isEqualTo("Licensed, never inspected.");
        assertThat(page.lastInspected()).isEqualTo("Never");
        assertThat(page.historySummary()).isNull();
    }

    @Test
    void riskIsRelabelledAsCadenceWithTheCitysOwnTermKept() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.OPEN, InspectionResult.PASS, LocalDate.of(2024, 3, 12),
                        Risk.HIGH, null),
                List.of());

        assertThat(page.cadence()).isEqualTo("Inspected most often");
        assertThat(page.cadenceNote())
                .contains("Risk 1 (High)")
                .contains("not how dangerous it is");
    }

    @Test
    void unknownRiskShowsNoCadenceAtAll() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.OPEN, InspectionResult.PASS, LocalDate.of(2024, 3, 12),
                        Risk.UNKNOWN, null),
                List.of());

        assertThat(page.cadence()).isNull();
        assertThat(page.cadenceNote()).isNull();
    }

    @Test
    void historySummarySpansOldestToNewest() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.OPEN, InspectionResult.PASS, LocalDate.of(2024, 3, 12),
                        Risk.HIGH, null),
                List.of(inspection(LocalDate.of(2024, 3, 12), InspectionResult.PASS),
                        inspection(LocalDate.of(2019, 5, 2), InspectionResult.FAIL)));

        assertThat(page.historySummary()).isEqualTo("2 inspections on record, 2019–2024.");
    }

    @Test
    void aSingleInspectionReadsAsOneYearAndSingularNoun() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.OPEN, InspectionResult.PASS, LocalDate.of(2024, 3, 12),
                        Risk.HIGH, null),
                List.of(inspection(LocalDate.of(2024, 3, 12), InspectionResult.PASS)));

        assertThat(page.historySummary()).isEqualTo("1 inspection on record, 2024.");
    }

    @Test
    void ledgerDatesAreAbbreviatedSoTheColumnNeverWraps() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.OPEN, InspectionResult.PASS, LocalDate.of(2023, 9, 18),
                        Risk.HIGH, null),
                List.of(inspection(LocalDate.of(2023, 9, 18), InspectionResult.PASS)));

        assertThat(page.rows()).singleElement()
                .extracting(EstablishmentPage.Row::date).isEqualTo("18 Sep 2023");
        // Prose keeps the spelled-out form.
        assertThat(page.lastInspected()).isEqualTo("18 September 2023");
    }

    @Test
    void resultClassSeparatesPassFailAndAdministrativeOutcomes() {
        var page = EstablishmentPage.of(
                view(EstablishmentStatus.OPEN, InspectionResult.PASS, LocalDate.of(2024, 3, 12),
                        Risk.HIGH, null),
                List.of(inspection(LocalDate.of(2024, 3, 12), InspectionResult.PASS),
                        inspection(LocalDate.of(2023, 3, 12), InspectionResult.PASS_WITH_CONDITIONS),
                        inspection(LocalDate.of(2022, 3, 12), InspectionResult.FAIL),
                        inspection(LocalDate.of(2021, 3, 12), InspectionResult.NO_ENTRY)));

        assertThat(page.rows()).extracting(EstablishmentPage.Row::resultClass)
                .containsExactly("badge-pass", "badge-pass", "badge-fail", "badge-neutral");
    }
}

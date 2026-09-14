package com.tedredington.bourdain.establishment.internal;

import java.time.LocalDate;
import java.util.List;

import com.tedredington.bourdain.establishment.EstablishmentStatus;
import com.tedredington.bourdain.establishment.EstablishmentView;
import com.tedredington.bourdain.establishment.FacilityCategory;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.InspectionResult;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EstablishmentsImplTest {

    private static EstablishmentView view(long license, String name, String address, LocalDate inspectedOn) {
        return new EstablishmentView(license, name, null, address, "CHICAGO", "60647",
                FacilityCategory.RESTAURANT, "Restaurant", Risk.HIGH, EstablishmentStatus.OPEN,
                InspectionResult.PASS, inspectedOn, null);
    }

    @Test
    void aBusinessWithSeveralLicensesAppearsOnceAsItsMostRecentLicense() {
        // Longman & Eagle: the food license, then a caterer's liquor license last inspected in 2011.
        List<EstablishmentView> results = EstablishmentsImpl.onePerPlace(List.of(
                view(80875, "LONGMAN & EAGLE", "2657 N KEDZIE AVE", LocalDate.of(2025, 11, 19)),
                view(2083925, "LONGMAN & EAGLE", "2657 N KEDZIE AVE ", LocalDate.of(2011, 6, 9))), 25);

        assertThat(results).extracting(EstablishmentView::licenseNumber).containsExactly(80875L);
    }

    @Test
    void aChainAtDifferentAddressesStaysSeparate() {
        List<EstablishmentView> results = EstablishmentsImpl.onePerPlace(List.of(
                view(1, "SUBWAY", "100 W RANDOLPH ST", LocalDate.of(2026, 9, 1)),
                view(2, "SUBWAY", "2400 N LINCOLN AVE", LocalDate.of(2026, 8, 1))), 25);

        assertThat(results).hasSize(2);
    }

    @Test
    void differentBusinessesAtOneAddressStaySeparate() {
        List<EstablishmentView> results = EstablishmentsImpl.onePerPlace(List.of(
                view(1, "HUDSON NEWS", "11601 W TOUHY AVE", LocalDate.of(2026, 9, 1)),
                view(2, "UNITED CLUB", "11601 W TOUHY AVE", LocalDate.of(2026, 8, 1))), 25);

        assertThat(results).hasSize(2);
    }

    @Test
    void theLimitCountsPlacesNotLicenses() {
        List<EstablishmentView> results = EstablishmentsImpl.onePerPlace(List.of(
                view(1, "TRIPLE A SERVICES", "2637 S THROOP ST", LocalDate.of(2026, 9, 1)),
                view(2, "TRIPLE A SERVICES", "2637 S THROOP ST", LocalDate.of(2026, 8, 1)),
                view(3, "TRIPLE A SERVICES", "2637 S THROOP ST", LocalDate.of(2026, 7, 1)),
                view(4, "BAMBI", "2051 W 47TH ST", LocalDate.of(2026, 9, 1)),
                view(5, "PALETERIA AZTECA #2", "3119 W CERMAK RD", LocalDate.of(2026, 9, 1))), 2);

        assertThat(results).extracting(EstablishmentView::licenseNumber).containsExactly(1L, 4L);
    }

    @Test
    void anOverlongQueryIsCutBeforeItReachesTheDatabase() {
        EstablishmentRepository repository = mock(EstablishmentRepository.class);

        new EstablishmentsImpl(repository).search("pequods ".repeat(50), 25);

        verify(repository).search(argThat(
                q -> q.length() <= EstablishmentsImpl.MAX_QUERY_LENGTH), anyInt());
    }
}

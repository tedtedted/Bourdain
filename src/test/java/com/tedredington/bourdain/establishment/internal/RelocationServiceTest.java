package com.tedredington.bourdain.establishment.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.tedredington.bourdain.civicdata.CivicDataSyncCompleted;
import com.tedredington.bourdain.civicdata.SyncSource;
import com.tedredington.bourdain.establishment.EstablishmentStatus;
import com.tedredington.bourdain.establishment.EstablishmentView.Relocation;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RelocationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

    private final EstablishmentRepository establishments = mock(EstablishmentRepository.class);
    private final BusinessLicenseRepository licenses = mock(BusinessLicenseRepository.class);
    private final RelocationService service = new RelocationService(establishments, licenses,
            Clock.fixed(NOW, ZoneId.of("America/Chicago")));

    @Test
    void ignoresInspectionSyncCompletionUntilLicensesAreRefreshed() {
        service.on(new CivicDataSyncCompleted(SyncSource.INSPECTIONS, 1L, 10));

        verifyNoInteractions(establishments, licenses);
    }

    @Test
    void delistsBeforeMatchingAndRelocatesWhatItFound() {
        var duke = new RelocationMatcher.Closed(18158, "2913 N CLARK ST", LocalDate.of(2024, 7, 16));
        var broadway = new RelocationMatcher.Candidate(2252464, "2827 N BROADWAY  1",
                LocalDate.of(2025, 2, 16), LocalDate.of(2027, 2, 15));
        Establishment dukeRow = EstablishmentTest.establishment(18158, 101, "THE DUKE OF PERTH",
                LocalDate.of(2024, 7, 16));
        when(establishments.findClosedWithNamesakes()).thenReturn(Map.of(duke, List.of(broadway)));
        when(establishments.findAllById(any())).thenReturn(List.of(dukeRow));

        service.on(new CivicDataSyncCompleted(SyncSource.LICENSES, 2L, 1));

        InOrder order = inOrder(licenses, establishments);
        order.verify(licenses).delistNotListedBy(2L, NOW);
        order.verify(establishments).resetStatuses();
        order.verify(establishments).findClosedWithNamesakes();
        order.verify(establishments).findAllById(any());
        order.verify(establishments).recordStatusChanges();
        assertThat(dukeRow.toView().status()).isEqualTo(EstablishmentStatus.RELOCATED);
        assertThat(dukeRow.toView().relocation())
                .isEqualTo(new Relocation(2252464, "2827 N BROADWAY  1", LocalDate.of(2025, 2, 16)));
    }
}

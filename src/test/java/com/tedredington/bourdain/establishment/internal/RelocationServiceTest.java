package com.tedredington.bourdain.establishment.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.tedredington.bourdain.civicdata.CivicDataSyncCompleted;
import com.tedredington.bourdain.civicdata.SyncSource;
import com.tedredington.bourdain.establishment.EstablishmentView.Relocation;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RelocationServiceTest {

    private static final Instant RUN_STARTED = Instant.parse("2026-08-24T11:30:00Z");

    private final EstablishmentRepository establishments = mock(EstablishmentRepository.class);
    private final BusinessLicenseRepository licenses = mock(BusinessLicenseRepository.class);
    private final RelocationService service = new RelocationService(establishments, licenses,
            Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneId.of("America/Chicago")));

    @Test
    void ignoresInspectionSyncCompletionUntilLicensesAreRefreshed() {
        service.on(new CivicDataSyncCompleted(SyncSource.INSPECTIONS, 1L, RUN_STARTED, 10));

        verifyNoInteractions(establishments, licenses);
    }

    @Test
    void delistsBeforeMatchingAndRecordsWhatItFound() {
        var duke = new RelocationMatcher.Closed(18158, "2913 N CLARK ST", LocalDate.of(2024, 7, 16));
        var broadway = new RelocationMatcher.Candidate(2252464, "2827 N BROADWAY  1",
                LocalDate.of(2025, 2, 16), LocalDate.of(2027, 2, 15));
        when(establishments.findClosedWithNamesakes()).thenReturn(Map.of(duke, List.of(broadway)));

        service.on(new CivicDataSyncCompleted(SyncSource.LICENSES, 2L, RUN_STARTED, 1));

        InOrder order = inOrder(licenses, establishments);
        order.verify(licenses).delistNotUpsertedSince(RUN_STARTED);
        order.verify(establishments).resetStatuses();
        order.verify(establishments).findClosedWithNamesakes();
        order.verify(establishments).markRelocated(
                Map.of(18158L, new Relocation(2252464, "2827 N BROADWAY  1", LocalDate.of(2025, 2, 16))));
        order.verify(establishments).recordStatusChanges();
    }
}

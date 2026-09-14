package com.tedredington.bourdain.establishment.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tedredington.bourdain.civicdata.CivicDataSyncCompleted;
import com.tedredington.bourdain.civicdata.SyncSource;
import com.tedredington.bourdain.establishment.EstablishmentView.Relocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/**
 * The sync → matching handoff. Runs after the license sync transaction commits
 * (Modulith registry event, so a crash here is retried on restart) and
 * recomputes every establishment's status from scratch — the derivation is
 * cheap and idempotence beats bookkeeping about what changed. The outcome is
 * then compared against each establishment's last recorded status, so the
 * timeline only grows when something actually changed.
 */
@Service
class RelocationService {

    private static final Logger log = LoggerFactory.getLogger(RelocationService.class);

    private final EstablishmentRepository establishments;
    private final BusinessLicenseRepository licenses;
    private final Clock clock;

    RelocationService(EstablishmentRepository establishments, BusinessLicenseRepository licenses, Clock clock) {
        this.establishments = establishments;
        this.licenses = licenses;
        this.clock = clock;
    }

    /**
     * {@link ApplicationModuleListener} runs this in a transaction of its own,
     * so delisting, the interim status reset, and the recorded changes commit
     * together; the reset is never visible or recorded on its own.
     */
    @ApplicationModuleListener
    void on(CivicDataSyncCompleted event) {
        if (event.source() != SyncSource.LICENSES) {
            return;
        }
        // The city drops lapsed licenses from the feed, so a license this run
        // didn't list is no longer listed. Matching only considers listed ones.
        int delisted = licenses.delistNotListedBy(event.runId(), clock.instant());
        log.info("Deriving establishment statuses after license sync ({} licenses delisted)", delisted);
        deriveStatuses();
    }

    private void deriveStatuses() {
        establishments.resetStatuses();

        Map<RelocationMatcher.Closed, List<RelocationMatcher.Candidate>> closed =
                establishments.findClosedWithNamesakes();
        LocalDate today = LocalDate.now(clock);
        Map<Long, Relocation> relocations = new LinkedHashMap<>();
        closed.forEach((establishment, candidates) -> RelocationMatcher.match(establishment, candidates, today)
                .ifPresent(relocation -> relocations.put(establishment.licenseNumber(), relocation)));
        Instant now = clock.instant();
        establishments.findAllById(relocations.keySet())
                .forEach(establishment -> establishment.relocateTo(relocations.get(establishment.getId()), now));

        int changes = establishments.recordStatusChanges();
        log.info("Status derivation done: {} closed establishments, {} marked relocated, {} status changes recorded",
                closed.size(), relocations.size(), changes);
    }
}

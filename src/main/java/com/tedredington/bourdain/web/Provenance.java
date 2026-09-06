package com.tedredington.bourdain.web;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.tedredington.bourdain.civicdata.SyncSource;
import com.tedredington.bourdain.civicdata.SyncStatus;
import com.tedredington.bourdain.establishment.Establishments;

import org.springframework.ui.Model;

/**
 * Footer provenance: when the mirror last caught up with the city, and how much
 * of it there is. Every page carries it — a page that makes a claim about a
 * business should say how fresh the data behind the claim is.
 */
final class Provenance {

    private static final DateTimeFormatter SYNC_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("America/Chicago"));

    private Provenance() {
    }

    static void addTo(Model model, SyncStatus syncStatus, Establishments establishments) {
        model.addAttribute("establishmentCount", establishments.count());
        model.addAttribute("lastSyncText", syncStatus.lastSuccessful(SyncSource.INSPECTIONS)
                .map(sync -> SYNC_TIME.format(sync.finishedAt()))
                .orElse(null));
        model.addAttribute("syncFailureText", syncStatus.lastAttempt(SyncSource.INSPECTIONS)
                .filter(SyncStatus.SyncAttempt::failed)
                .map(attempt -> SYNC_TIME.format(attempt.finishedAt()))
                .orElse(null));
    }
}

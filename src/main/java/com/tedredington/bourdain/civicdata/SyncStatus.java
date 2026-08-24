package com.tedredington.bourdain.civicdata;

import java.time.Instant;
import java.util.Optional;

/** Read-side view of sync history, e.g. for the site footer. */
public interface SyncStatus {

    Optional<LastSync> lastSuccessful(SyncSource source);

    record LastSync(Instant finishedAt, int rowsUpserted) {
    }
}

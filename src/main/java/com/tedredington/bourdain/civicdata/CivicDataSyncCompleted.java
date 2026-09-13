package com.tedredington.bourdain.civicdata;

import java.time.Instant;

/**
 * A sync run finished successfully. Persisted via the Modulith JDBC event
 * publication registry, so downstream derivation (establishment status,
 * relocation matching) is retried on restart if it didn't complete.
 *
 * @param startedAt when the run began, by the database clock. Every row the
 *                  run wrote is stamped later.
 */
public record CivicDataSyncCompleted(SyncSource source, long runId, Instant startedAt, int rowsUpserted) {
}

package com.tedredington.bourdain.civicdata;

/**
 * A sync run finished successfully. Persisted via the Modulith JDBC event
 * publication registry, so downstream derivation (establishment status,
 * relocation matching) is retried on restart if it didn't complete.
 */
public record CivicDataSyncCompleted(SyncSource source, long runId, int rowsUpserted) {
}

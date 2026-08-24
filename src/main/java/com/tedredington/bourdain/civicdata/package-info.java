/**
 * Acquisition of City of Chicago open data (Socrata/SODA API).
 *
 * <p>Owns the sync schedule, incremental watermarks, and {@code sync_run}
 * bookkeeping. Fetched rows are handed to the domain modules as in-transaction
 * {@link com.tedredington.bourdain.civicdata.InspectionBatchReceived} /
 * {@link com.tedredington.bourdain.civicdata.LicenseBatchReceived} events, and a
 * persisted {@link com.tedredington.bourdain.civicdata.CivicDataSyncCompleted}
 * event (JDBC event publication registry) triggers downstream derivation once a
 * run finishes. This module depends on no other module.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Civic Data Sync")
package com.tedredington.bourdain.civicdata;

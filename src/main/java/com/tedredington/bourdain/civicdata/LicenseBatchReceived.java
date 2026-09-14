package com.tedredington.bourdain.civicdata;

import java.util.List;

/**
 * Page of active-license rows; see {@link InspectionBatchReceived} for semantics.
 *
 * @param syncRunId the run listing these licenses; once it completes, anything
 *                  a run of this id didn't list is delisted
 */
public record LicenseBatchReceived(long syncRunId, List<LicenseRecord> records) {
}

package com.tedredington.bourdain.civicdata;

import java.util.List;

/** Page of active-license rows; see {@link InspectionBatchReceived} for semantics. */
public record LicenseBatchReceived(List<LicenseRecord> records) {
}

package com.tedredington.bourdain.civicdata.internal;

import java.util.List;

import com.tedredington.bourdain.civicdata.InspectionRecord;
import com.tedredington.bourdain.civicdata.LicenseRecord;

/**
 * Seam over the upstream open-data API. The Socrata implementation is the only
 * production one; tests substitute canned pages.
 */
public interface CivicDataSource {

    /**
     * One keyset page of inspections, ordered by Socrata row id. Passing the
     * previous page's {@code lastRowId} continues the scan; {@code updatedSince}
     * (a SODA floating timestamp, nullable on first run) limits the scan to rows
     * touched after the last successful sync.
     */
    InspectionPage inspectionsPage(String updatedSince, String lastRowId, int pageSize);

    /** One keyset page of active food-related licenses, ordered by row id. */
    LicensePage licensesPage(String lastRowId, int pageSize);

    record InspectionPage(List<InspectionRecord> records, String lastRowId, String maxUpdatedAt, int skipped) {
    }

    record LicensePage(List<LicenseRecord> records, String lastRowId, int skipped) {
    }
}

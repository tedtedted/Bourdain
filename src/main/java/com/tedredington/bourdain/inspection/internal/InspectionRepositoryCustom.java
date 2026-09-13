package com.tedredington.bourdain.inspection.internal;

import java.util.List;

/** Batch upsert with revisions, and history loading the aggregate in two queries rather than one per inspection. */
interface InspectionRepositoryCustom {

    /**
     * Idempotent: re-upserting an inspection writes the same row and rebuilds
     * its violations. The version an amended inspection replaces is kept as a
     * revision.
     */
    void upsertAll(List<Inspection> inspections);

    /**
     * Newest first, violations in the order the inspector listed them. A
     * derived query would select each inspection's violations separately.
     */
    List<Inspection> findHistory(long licenseNumber);
}

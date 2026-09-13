package com.tedredington.bourdain.establishment.internal;

import java.util.List;
import java.util.Map;

import com.tedredington.bourdain.establishment.EstablishmentView.Relocation;

/** Bulk writes and status derivation, which need SQL a derived query can't express. */
interface EstablishmentRepositoryCustom {

    /**
     * Upserts each snapshot unless the stored row reflects a newer inspection.
     * Details a snapshot replaces are kept as a revision.
     */
    void upsertLatest(List<EstablishmentSnapshot> snapshots);

    /** Sets every establishment to OPEN or CLOSED from its latest result and clears relocations. */
    void resetStatuses();

    /** Each CLOSED establishment, with the listed licenses of other license numbers sharing its name. */
    Map<RelocationMatcher.Closed, List<RelocationMatcher.Candidate>> findClosedWithNamesakes();

    /** Keyed by the closed establishment's license number. */
    void markRelocated(Map<Long, Relocation> relocations);

    /** Appends a timeline row for every establishment whose status or relocation target changed. */
    int recordStatusChanges();
}

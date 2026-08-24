package com.tedredington.bourdain.establishment;

import java.util.List;
import java.util.Optional;

/** Read-side API of the establishment module. */
public interface Establishments {

    Optional<EstablishmentView> byLicenseNumber(long licenseNumber);

    /**
     * Name/address search; a bare five-digit query is treated as a ZIP code.
     * Results are ordered by name similarity, then recency.
     */
    List<EstablishmentView> search(String query, int limit);

    long count();
}

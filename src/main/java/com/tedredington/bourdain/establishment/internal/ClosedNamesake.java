package com.tedredington.bourdain.establishment.internal;

import java.time.LocalDate;

/** A closed establishment paired with one listed license sharing its normalized name. */
record ClosedNamesake(
        long closedLicenseNumber,
        String closedAddress,
        LocalDate lastInspectedOn,
        long candidateLicenseNumber,
        String candidateAddress,
        LocalDate candidateStartDate,
        LocalDate candidateExpirationDate) {

    RelocationMatcher.Closed closed() {
        return new RelocationMatcher.Closed(closedLicenseNumber, closedAddress, lastInspectedOn);
    }

    RelocationMatcher.Candidate candidate() {
        return new RelocationMatcher.Candidate(candidateLicenseNumber, candidateAddress, candidateStartDate,
                candidateExpirationDate);
    }
}

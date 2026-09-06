package com.tedredington.bourdain.establishment.internal;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.tedredington.bourdain.establishment.EstablishmentView.Relocation;

/**
 * Decides whether a closed establishment actually moved. Candidates are active
 * licenses sharing the establishment's normalized name; a match requires:
 *
 * <ul>
 *   <li>a different location (house number) than the closed one — suite
 *       renumbering at the same address is not a move;</li>
 *   <li>a license that is current and not started absurdly before the closure
 *       (operators often license the new spot months before the old one is
 *       marked out of business, hence the grace window);</li>
 *   <li>exactly one surviving location. Multiple locations means a chain
 *       ("SUBWAY"), where name identity says nothing about relocation.</li>
 * </ul>
 */
public final class RelocationMatcher {

    /** How long before the closure a new location's license may have started. */
    private static final long GRACE_DAYS = 365;

    private RelocationMatcher() {
    }

    public record Closed(long licenseNumber, String address, LocalDate lastInspectedOn) {
    }

    public record Candidate(long licenseNumber, String address, LocalDate startDate, LocalDate expirationDate) {
    }

    public static Optional<Relocation> match(Closed closed, List<Candidate> candidates, LocalDate today) {
        List<Candidate> plausible = candidates.stream()
                .filter(c -> c.licenseNumber() != closed.licenseNumber())
                .filter(c -> !Addresses.sameLocation(c.address(), closed.address()))
                .filter(c -> c.expirationDate() == null || !c.expirationDate().isBefore(today))
                .filter(c -> c.startDate() == null || closed.lastInspectedOn() == null
                        || !c.startDate().isBefore(closed.lastInspectedOn().minusDays(GRACE_DAYS)))
                .toList();

        long distinctLocations = plausible.stream()
                .map(c -> Addresses.locationKey(c.address()))
                .distinct()
                .count();
        if (distinctLocations != 1) {
            return Optional.empty();
        }

        // Several licenses at the one location (food + tavern, renewals):
        // report the most recently started one.
        return plausible.stream()
                .max(Comparator.comparing(Candidate::startDate,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Candidate::licenseNumber))
                .map(c -> new Relocation(c.licenseNumber(), c.address(), c.startDate()));
    }
}

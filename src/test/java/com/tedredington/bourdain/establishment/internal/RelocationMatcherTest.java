package com.tedredington.bourdain.establishment.internal;

import java.time.LocalDate;
import java.util.List;

import com.tedredington.bourdain.establishment.EstablishmentView.Relocation;
import com.tedredington.bourdain.establishment.internal.RelocationMatcher.Candidate;
import com.tedredington.bourdain.establishment.internal.RelocationMatcher.Closed;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelocationMatcherTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 24);

    // The real Duke of Perth timeline: last inspected (Out of Business) July
    // 2024 at Clark St, re-licensed Feb 2025 on Broadway under two licenses.
    private final Closed dukeOfPerth =
            new Closed(18158, "2913 N CLARK ST ", LocalDate.of(2024, 7, 16));

    @Test
    void singleNewLocationIsARelocation() {
        List<Candidate> candidates = List.of(
                new Candidate(2252464, "2827 N BROADWAY  1", LocalDate.of(2025, 2, 16), LocalDate.of(2027, 2, 15)),
                new Candidate(2252465, "2827 N BROADWAY  1", LocalDate.of(2025, 2, 16), LocalDate.of(2027, 2, 15)));

        assertThat(RelocationMatcher.match(dukeOfPerth, candidates, TODAY))
                .contains(new Relocation(2252465, "2827 N BROADWAY  1", LocalDate.of(2025, 2, 16)));
    }

    @Test
    void chainsWithManyLocationsAreNotRelocations() {
        List<Candidate> candidates = List.of(
                new Candidate(1, "100 W MADISON ST", LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1)),
                new Candidate(2, "200 S STATE ST", LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1)));

        assertThat(RelocationMatcher.match(dukeOfPerth, candidates, TODAY)).isEmpty();
    }

    @Test
    void licensesAtTheSameAddressAreNotRelocations() {
        List<Candidate> candidates = List.of(
                new Candidate(999, "2913 N CLARK ST 1ST", LocalDate.of(2025, 5, 16), LocalDate.of(2027, 5, 15)));

        assertThat(RelocationMatcher.match(dukeOfPerth, candidates, TODAY)).isEmpty();
    }

    @Test
    void expiredAndAncientLicensesAreIgnored() {
        List<Candidate> candidates = List.of(
                // expired before today
                new Candidate(1, "500 W ELM ST", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 9, 1)),
                // started years before the closure: an unrelated namesake
                new Candidate(2, "600 W OAK ST", LocalDate.of(2019, 1, 1), LocalDate.of(2027, 1, 1)));

        assertThat(RelocationMatcher.match(dukeOfPerth, candidates, TODAY)).isEmpty();
    }
}

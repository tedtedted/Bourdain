package com.tedredington.bourdain.establishment.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameNormalizerTest {

    @Test
    void matchesTheTwoDatasetsSpellingsOfTheSameBusiness() {
        // Inspections say "THE DUKE OF PERTH"; the license feed says "Duke of Perth".
        assertThat(NameNormalizer.normalize("THE DUKE OF PERTH"))
                .isEqualTo(NameNormalizer.normalize("Duke of Perth"))
                .isEqualTo("duke of perth");
    }

    @Test
    void stripsPunctuationAndCollapsesWhitespace() {
        assertThat(NameNormalizer.normalize("Lou Malnati's  Pizzeria, Inc.")).isEqualTo("lou malnati s pizzeria inc");
        assertThat(NameNormalizer.normalize(null)).isEmpty();
    }
}

package com.tedredington.bourdain.inspection.internal;

import java.util.List;

import com.tedredington.bourdain.inspection.Inspections.ViolationLine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ViolationParserTest {

    @Test
    void parsesMultipleViolationsWithComments() {
        String raw = "3. MANAGEMENT, FOOD EMPLOYEE AND CONDITIONAL EMPLOYEE - Comments: OBSERVED NO CERTIFICATE. "
                + "| 38. INSECTS, RODENTS, & ANIMALS NOT PRESENT - Comments: OBSERVED RODENT DROPPINGS.";

        List<ViolationLine> lines = ViolationParser.parse(raw);

        assertThat(lines).containsExactly(
                new ViolationLine(3, "MANAGEMENT, FOOD EMPLOYEE AND CONDITIONAL EMPLOYEE",
                        "OBSERVED NO CERTIFICATE."),
                new ViolationLine(38, "INSECTS, RODENTS, & ANIMALS NOT PRESENT",
                        "OBSERVED RODENT DROPPINGS."));
    }

    @Test
    void commentIsOptional() {
        List<ViolationLine> lines = ViolationParser.parse("18. NO EVIDENCE OF RODENT OR INSECT INFESTATION");

        assertThat(lines).containsExactly(
                new ViolationLine(18, "NO EVIDENCE OF RODENT OR INSECT INFESTATION", null));
    }

    @Test
    void descriptionMayContainDashes() {
        List<ViolationLine> lines =
                ViolationParser.parse("47. FOOD & NON-FOOD CONTACT SURFACES - CLEANABLE - Comments: GREASE BUILDUP");

        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().code()).isEqualTo(47);
        assertThat(lines.getFirst().description()).isEqualTo("FOOD & NON-FOOD CONTACT SURFACES - CLEANABLE");
        assertThat(lines.getFirst().comment()).isEqualTo("GREASE BUILDUP");
    }

    @Test
    void blankAndUnparseableInputYieldNothing() {
        assertThat(ViolationParser.parse(null)).isEmpty();
        assertThat(ViolationParser.parse("  ")).isEmpty();
        assertThat(ViolationParser.parse("no leading number here")).isEmpty();
    }
}

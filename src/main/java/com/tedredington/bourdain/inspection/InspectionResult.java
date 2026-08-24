package com.tedredington.bourdain.inspection;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Outcome of an inspection. The dataset's {@code results} column is a closed
 * set of seven strings; anything unrecognized maps to {@link #UNKNOWN} rather
 * than failing the sync.
 */
public enum InspectionResult {

    PASS("Pass"),
    PASS_WITH_CONDITIONS("Pass w/ Conditions"),
    FAIL("Fail"),
    OUT_OF_BUSINESS("Out of Business"),
    NO_ENTRY("No Entry"),
    NOT_READY("Not Ready"),
    BUSINESS_NOT_LOCATED("Business Not Located"),
    UNKNOWN("Unknown");

    private static final Map<String, InspectionResult> BY_RAW = Stream.of(values())
            .collect(Collectors.toMap(r -> r.label.toLowerCase(), Function.identity()));

    private final String label;

    InspectionResult(String label) {
        this.label = label;
    }

    public static InspectionResult fromRaw(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return BY_RAW.getOrDefault(raw.trim().toLowerCase(), UNKNOWN);
    }

    public String label() {
        return label;
    }

    /** Pass and conditional pass both count as passing for display purposes. */
    public boolean passing() {
        return this == PASS || this == PASS_WITH_CONDITIONS;
    }
}

package com.tedredington.bourdain.inspection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class InspectionEnumsTest {

    @ParameterizedTest
    @CsvSource({
            "Pass, PASS",
            "pass w/ conditions, PASS_WITH_CONDITIONS",
            "FAIL, FAIL",
            "Out of Business, OUT_OF_BUSINESS",
            "No Entry, NO_ENTRY",
            "Not Ready, NOT_READY",
            "Business Not Located, BUSINESS_NOT_LOCATED",
    })
    void mapsEveryKnownResult(String raw, InspectionResult expected) {
        assertThat(InspectionResult.fromRaw(raw)).isEqualTo(expected);
    }

    @Test
    void unknownResultsDoNotBreakTheSync() {
        assertThat(InspectionResult.fromRaw("Some Future Value")).isEqualTo(InspectionResult.UNKNOWN);
        assertThat(InspectionResult.fromRaw(null)).isEqualTo(InspectionResult.UNKNOWN);
    }

    @ParameterizedTest
    @CsvSource({
            "Canvass, CANVASS",
            "Canvass Re-Inspection, CANVASS_REINSPECTION",
            "CANVASS RE INSPECTION, CANVASS_REINSPECTION",
            "License, LICENSE",
            "License Re-Inspection, LICENSE_REINSPECTION",
            "License-Task Force, TASK_FORCE",
            "Task Force Liquor 1475, TASK_FORCE",
            "Complaint, COMPLAINT",
            "Complaint Re-Inspection, COMPLAINT_REINSPECTION",
            "Short Form Complaint, SHORT_FORM_COMPLAINT",
            "Suspected Food Poisoning, SUSPECTED_FOOD_POISONING",
            "Suspected Food Poisoning Re-inspection, SUSPECTED_FOOD_POISONING_REINSPECTION",
            "OUT OF BUSINESS, OUT_OF_BUSINESS",
            "Special Events (Festivals), SPECIAL_EVENT",
            "Sushi Rollout, OTHER",
    })
    void classifiesMessyInspectionTypes(String raw, InspectionType expected) {
        assertThat(InspectionType.classify(raw)).isEqualTo(expected);
    }
}

package com.tedredington.bourdain.inspection;

import java.util.Locale;

/**
 * Why the inspection happened. Unlike {@code results}, the source column is
 * free text with well over a hundred spellings, so classification is
 * normalized-keyword matching with an {@link #OTHER} fallback; the raw value is
 * stored alongside for display.
 */
public enum InspectionType {

    CANVASS("Routine canvass"),
    CANVASS_REINSPECTION("Canvass re-inspection"),
    LICENSE("License"),
    LICENSE_REINSPECTION("License re-inspection"),
    COMPLAINT("Complaint"),
    COMPLAINT_REINSPECTION("Complaint re-inspection"),
    SHORT_FORM_COMPLAINT("Short form complaint"),
    SUSPECTED_FOOD_POISONING("Suspected food poisoning"),
    SUSPECTED_FOOD_POISONING_REINSPECTION("Food poisoning re-inspection"),
    CONSULTATION("Consultation"),
    TASK_FORCE("Task force"),
    TAG_REMOVAL("Tag removal"),
    NON_INSPECTION("Non-inspection"),
    OUT_OF_BUSINESS("Out of business"),
    RECENT_INSPECTION("Recent inspection"),
    SPECIAL_EVENT("Special event"),
    NO_ENTRY("No entry"),
    NOT_READY("Not ready"),
    OTHER("Other");

    private final String label;

    InspectionType(String label) {
        this.label = label;
    }

    public static InspectionType classify(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        // Collapse case, punctuation, and spacing so "Re-Inspection",
        // "RE-INSPECTION", and "reinspection" all compare equal.
        String n = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim()
                .replace("re inspection", "reinspection");

        boolean reinspection = n.contains("reinspection");
        if (n.contains("canvass") || n.contains("canvas")) {
            return reinspection ? CANVASS_REINSPECTION : CANVASS;
        }
        if (n.contains("food poisoning")) {
            return reinspection ? SUSPECTED_FOOD_POISONING_REINSPECTION : SUSPECTED_FOOD_POISONING;
        }
        if (n.contains("short form") && n.contains("complaint")) {
            return SHORT_FORM_COMPLAINT;
        }
        if (n.contains("complaint")) {
            return reinspection ? COMPLAINT_REINSPECTION : COMPLAINT;
        }
        if (n.contains("task force") || n.contains("liquor 14")) {
            return TASK_FORCE;
        }
        if (n.contains("license")) {
            return reinspection ? LICENSE_REINSPECTION : LICENSE;
        }
        if (n.contains("consultation")) {
            return CONSULTATION;
        }
        if (n.contains("tag removal")) {
            return TAG_REMOVAL;
        }
        if (n.contains("non inspection")) {
            return NON_INSPECTION;
        }
        if (n.contains("out of business")) {
            return OUT_OF_BUSINESS;
        }
        if (n.contains("recent inspection")) {
            return RECENT_INSPECTION;
        }
        if (n.contains("special event") || n.contains("festival")) {
            return SPECIAL_EVENT;
        }
        if (n.contains("no entry")) {
            return NO_ENTRY;
        }
        if (n.contains("not ready")) {
            return NOT_READY;
        }
        return OTHER;
    }

    public String label() {
        return label;
    }
}

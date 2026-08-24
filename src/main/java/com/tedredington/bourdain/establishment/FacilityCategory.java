package com.tedredington.bourdain.establishment;

import java.util.Locale;

/**
 * Coarse grouping of the dataset's free-text {@code facility_type} (hundreds
 * of spellings). The raw value is kept on the establishment for display;
 * this enum exists for filtering and badges.
 */
public enum FacilityCategory {

    RESTAURANT("Restaurant"),
    GROCERY("Grocery"),
    BAKERY("Bakery"),
    BAR_TAVERN("Bar / Tavern"),
    CAFE("Café / Coffee"),
    SCHOOL("School"),
    DAYCARE("Daycare"),
    HOSPITAL_CARE("Hospital / Care"),
    MOBILE_FOOD("Mobile food"),
    CATERING("Catering"),
    OTHER("Other");

    private final String label;

    FacilityCategory(String label) {
        this.label = label;
    }

    public static FacilityCategory classify(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        String n = raw.toLowerCase(Locale.ROOT);
        if (n.contains("restaurant")) {
            return RESTAURANT;
        }
        if (n.contains("grocery") || n.contains("market")) {
            return GROCERY;
        }
        if (n.contains("bakery")) {
            return BAKERY;
        }
        if (n.contains("tavern") || n.matches(".*\\bbar\\b.*") || n.contains("liquor") || n.contains("brew")) {
            return BAR_TAVERN;
        }
        if (n.contains("coffee") || n.contains("cafe") || n.contains("café")) {
            return CAFE;
        }
        if (n.contains("school") || n.contains("college")) {
            return SCHOOL;
        }
        if (n.contains("daycare") || n.contains("day care") || n.contains("children")) {
            return DAYCARE;
        }
        if (n.contains("hospital") || n.contains("nursing") || n.contains("long term") || n.contains("assisted")) {
            return HOSPITAL_CARE;
        }
        if (n.contains("mobile")) {
            return MOBILE_FOOD;
        }
        if (n.contains("cater")) {
            return CATERING;
        }
        return OTHER;
    }

    public String label() {
        return label;
    }
}

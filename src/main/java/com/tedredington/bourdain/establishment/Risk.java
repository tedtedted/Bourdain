package com.tedredington.bourdain.establishment;

/**
 * The health department's inspection-frequency tier ("Risk 1 (High)" means
 * inspected most often, not most dangerous).
 */
public enum Risk {

    HIGH("Risk 1 (High)"),
    MEDIUM("Risk 2 (Medium)"),
    LOW("Risk 3 (Low)"),
    ALL("All"),
    UNKNOWN("Unknown");

    private final String label;

    Risk(String label) {
        this.label = label;
    }

    public static Risk fromRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        for (Risk risk : values()) {
            if (risk.label.equalsIgnoreCase(raw.trim())) {
                return risk;
            }
        }
        return UNKNOWN;
    }

    public String label() {
        return label;
    }
}

package com.tedredington.bourdain.inspection.internal;

import com.tedredington.bourdain.inspection.Inspections.ViolationLine;

/** Part of the {@link Inspection} aggregate; its position in the list is the {@code ordinal} column. */
record Violation(int code, String description, String comment) {

    static Violation of(ViolationLine line) {
        return new Violation(line.code(), line.description(), line.comment());
    }

    ViolationLine toLine() {
        return new ViolationLine(code, description, comment);
    }
}

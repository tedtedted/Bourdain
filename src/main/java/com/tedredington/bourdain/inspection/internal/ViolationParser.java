package com.tedredington.bourdain.inspection.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tedredington.bourdain.inspection.Inspections.ViolationLine;

/**
 * Parses the dataset's {@code violations} blob:
 * {@code "N. DESCRIPTION - Comments: FREE TEXT | N. DESCRIPTION - Comments: ..."}.
 * The comment part is optional; segments that don't match the shape are skipped.
 */
public final class ViolationParser {

    private static final Pattern SEGMENT =
            Pattern.compile("^\\s*(\\d+)\\.\\s*(.+?)(?:\\s*-\\s*Comments:\\s*(.*))?$", Pattern.DOTALL);

    private ViolationParser() {
    }

    public static List<ViolationLine> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<ViolationLine> lines = new ArrayList<>();
        for (String segment : raw.split("\\s*\\|\\s*")) {
            Matcher m = SEGMENT.matcher(segment.strip());
            if (!m.matches()) {
                continue;
            }
            String comment = m.group(3);
            lines.add(new ViolationLine(
                    Integer.parseInt(m.group(1)),
                    m.group(2).strip(),
                    comment == null || comment.isBlank() ? null : comment.strip()));
        }
        return lines;
    }
}

package com.tedredington.bourdain.web;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.tedredington.bourdain.establishment.EstablishmentView;
import com.tedredington.bourdain.establishment.Risk;
import com.tedredington.bourdain.inspection.Inspections;

/**
 * Everything the establishment template renders, phrased here rather than in
 * Thymeleaf. The page's job is to state what the record supports and no more,
 * so the wording of each claim is worth testing.
 */
record EstablishmentPage(
        EstablishmentView establishment,
        Standing standing,
        String cadence,
        String cadenceNote,
        String lastInspected,
        String historySummary,
        List<Row> rows) {

    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.US);

    /** The ledger is a fixed column; a spelled-out month wraps and breaks the alignment. */
    private static final DateTimeFormatter SHORT_DATE =
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.US);

    /**
     * The claim the page is willing to make, and the evidence behind it.
     * {@code tone} drives the CSS modifier only.
     */
    record Standing(String claim, String evidence, String tone) {
    }

    /** One inspection in the ledger, with its display strings resolved. */
    record Row(Inspections.InspectionDetail detail, String date, String resultClass) {
    }

    static EstablishmentPage of(EstablishmentView e, List<Inspections.InspectionDetail> history) {
        return new EstablishmentPage(
                e,
                standingOf(e),
                cadenceOf(e.risk()),
                cadenceNoteOf(e.risk()),
                e.lastInspectedOn() == null ? "Never" : LONG_DATE.format(e.lastInspectedOn()),
                historySummaryOf(history),
                history.stream().map(EstablishmentPage::rowOf).toList());
    }

    private static Standing standingOf(EstablishmentView e) {
        return switch (e.status()) {
            case RELOCATED -> new Standing(
                    "Moved, not closed.",
                    relocationEvidence(e),
                    "relocated");
            case CLOSED -> new Standing(
                    "Last recorded out of business.",
                    closedEvidence(e.lastInspectedOn()),
                    "closed");
            case OPEN -> openStanding(e);
        };
    }

    private static String relocationEvidence(EstablishmentView e) {
        var relocation = e.relocation();
        if (relocation == null) {
            return "An active license under this name sits at a different address.";
        }
        var sentence = new StringBuilder(
                "An inspection at this address recorded the business as closed. "
                        + "The same name holds an active license at ")
                .append(relocation.address());
        if (relocation.since() != null) {
            sentence.append(", licensed since ").append(LONG_DATE.format(relocation.since()));
        }
        return sentence.append(".").toString();
    }

    private static String closedEvidence(LocalDate lastInspectedOn) {
        String when = lastInspectedOn == null
                ? "An inspection here"
                : "The inspection here on " + LONG_DATE.format(lastInspectedOn);
        return when + " recorded this license as out of business. No active license "
                + "under this name matches a different address, so it reads as closed "
                + "rather than moved.";
    }

    private static Standing openStanding(EstablishmentView e) {
        if (e.lastInspectedOn() == null) {
            return new Standing(
                    "Licensed, never inspected.",
                    "Nothing is on record for this license. The city's inspection feed only "
                            + "covers licenses it has actually visited.",
                    "open");
        }
        String result = e.latestResult() == null ? null : e.latestResult().label();
        String evidence = "Nothing in the inspection record closes this license. Last inspected "
                + LONG_DATE.format(e.lastInspectedOn())
                + (result == null ? "." : " — " + result + ".");
        return new Standing("Open.", evidence, "open");
    }

    /**
     * The city's "Risk 1 (High)" is an inspection-frequency tier, not a danger
     * rating, and reads as the opposite to most people. Say what it means.
     */
    private static String cadenceOf(Risk risk) {
        if (risk == null) {
            return null;
        }
        return switch (risk) {
            case HIGH -> "Inspected most often";
            case MEDIUM -> "Inspected less often";
            case LOW -> "Inspected least often";
            case ALL, UNKNOWN -> null;
        };
    }

    private static String cadenceNoteOf(Risk risk) {
        String cadence = cadenceOf(risk);
        if (cadence == null) {
            return null;
        }
        return "The city files this as “" + risk.label()
                + "” — how often it inspects, not how dangerous it is.";
    }

    private static String historySummaryOf(List<Inspections.InspectionDetail> history) {
        if (history.isEmpty()) {
            return null;
        }
        int count = history.size();
        String noun = count == 1 ? "inspection" : "inspections";
        // history arrives newest first.
        int newest = history.getFirst().inspectedOn().getYear();
        int oldest = history.getLast().inspectedOn().getYear();
        String span = newest == oldest ? String.valueOf(newest) : oldest + "–" + newest;
        return count + " " + noun + " on record, " + span + ".";
    }

    private static Row rowOf(Inspections.InspectionDetail detail) {
        return new Row(detail, SHORT_DATE.format(detail.inspectedOn()), resultClassOf(detail));
    }

    private static String resultClassOf(Inspections.InspectionDetail detail) {
        if (detail.result() == null) {
            return "badge-neutral";
        }
        if (detail.result().passing()) {
            return "badge-pass";
        }
        return switch (detail.result()) {
            case FAIL -> "badge-fail";
            default -> "badge-neutral";
        };
    }
}

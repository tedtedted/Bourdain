package com.tedredington.bourdain.establishment.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Address comparison for relocation matching. The two datasets format the same
 * address differently ("2913 N CLARK ST " vs "2913 N CLARK ST 1ST"), so suite
 * and floor suffixes are ignored while the street core is preserved.
 */
public final class Addresses {

    private static final Pattern HOUSE_NUMBER = Pattern.compile("^\\s*(\\d+)");
    private static final Pattern NUMBER_AND_STREET = Pattern.compile("^(\\d+)\\s+(.+)$");

    private Addresses() {
    }

    /** Leading house number, or the whole address if there isn't one. */
    public static String houseNumber(String address) {
        if (address == null) {
            return "";
        }
        Matcher m = HOUSE_NUMBER.matcher(address);
        return m.find() ? m.group(1) : address.trim().toUpperCase();
    }

    public static String locationKey(String address) {
        String normalized = normalize(address);
        Matcher m = NUMBER_AND_STREET.matcher(normalized);
        if (!m.find()) {
            return normalized;
        }

        String street = m.group(2)
                .replaceFirst("\\s+(APT|APARTMENT|UNIT|STE|SUITE|#|FL|FLOOR|RM|ROOM)\\b.*$", "")
                .replaceFirst("\\s+\\d+(ST|ND|RD|TH)?$", "");
        return (m.group(1) + " " + street).trim();
    }

    public static boolean sameLocation(String a, String b) {
        return locationKey(a).equals(locationKey(b));
    }

    private static String normalize(String address) {
        if (address == null) {
            return "";
        }
        return address.toUpperCase()
                .replaceAll("[^A-Z0-9#]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}

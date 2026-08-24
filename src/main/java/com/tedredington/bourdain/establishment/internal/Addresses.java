package com.tedredington.bourdain.establishment.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Address comparison for relocation matching. The two datasets format the same
 * address differently ("2913 N CLARK ST " vs "2913 N CLARK ST 1ST"), so
 * "same location" is judged by the leading house number: suite suffixes don't
 * make a new location, a new house number does. A same-number move to a
 * different street is a known, rare blind spot.
 */
public final class Addresses {

    private static final Pattern HOUSE_NUMBER = Pattern.compile("^\\s*(\\d+)");

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

    public static boolean sameLocation(String a, String b) {
        return houseNumber(a).equals(houseNumber(b));
    }
}

package com.tedredington.bourdain.establishment.internal;

import java.util.Locale;

/**
 * Canonical form for matching business names across datasets: the inspections
 * feed has "THE DUKE OF PERTH" where the license feed has "Duke of Perth".
 */
public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String n = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        if (n.startsWith("the ")) {
            n = n.substring(4);
        }
        return n.replaceAll("\\s+", " ");
    }
}

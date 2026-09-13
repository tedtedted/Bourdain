package com.tedredington.bourdain.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The fallback for browsers without light-dark() repeats every pair by hand; this keeps the copies honest. */
class ThemeTokensTest {

    private static final Pattern DECLARATION = Pattern.compile("--([\\w-]+):\\s*([^;]+);");

    private static String css;
    private static Map<String, String> light;
    private static Map<String, String> dark;

    @BeforeAll
    static void readPairs() throws IOException {
        try (InputStream in = ThemeTokensTest.class.getResourceAsStream("/static/style.css")) {
            css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        light = new LinkedHashMap<>();
        dark = new LinkedHashMap<>();
        var matcher = DECLARATION.matcher(css);
        while (matcher.find()) {
            String value = matcher.group(2).strip();
            if (value.startsWith("light-dark(")) {
                String[] sides = splitTopLevelComma(value.substring("light-dark(".length(), value.length() - 1));
                light.put(matcher.group(1), sides[0]);
                dark.put(matcher.group(1), sides[1]);
            }
        }
    }

    @Test
    void everyNeutralIsAPair() {
        assertThat(light).containsKeys("ground", "surface", "ink", "muted", "rose", "rule");
    }

    @Test
    void fallbackLightMatchesThePairs() {
        assertThat(declarations(block(fallback(), ":root {"))).isEqualTo(light);
    }

    @Test
    void fallbackDarkMatchesThePairsForTheSystemAndThePinnedTheme() {
        String fallback = fallback();
        assertThat(declarations(block(fallback, ":root:not([data-theme=\"light\"]) {"))).isEqualTo(dark);
        assertThat(declarations(block(fallback, ":root[data-theme=\"dark\"] {"))).isEqualTo(dark);
    }

    private static String fallback() {
        return block(css, "@supports not (color: light-dark(#000, #fff)) {");
    }

    /** The body of the brace block opened by {@code opener}. */
    private static String block(String source, String opener) {
        int start = source.indexOf(opener);
        assertThat(start).as("block %s", opener).isNotNegative();
        int open = start + opener.length();
        int depth = 1;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return source.substring(open, i);
        }
        throw new IllegalStateException("Unclosed block " + opener);
    }

    private static Map<String, String> declarations(String body) {
        var found = new LinkedHashMap<String, String>();
        var matcher = DECLARATION.matcher(body);
        while (matcher.find()) found.put(matcher.group(1), matcher.group(2).strip());
        return found;
    }

    private static String[] splitTopLevelComma(String args) {
        int depth = 0;
        for (int i = 0; i < args.length(); i++) {
            switch (args.charAt(i)) {
                case '(' -> depth++;
                case ')' -> depth--;
                case ',' -> {
                    if (depth == 0) return new String[] {args.substring(0, i).strip(), args.substring(i + 1).strip()};
                }
                default -> { }
            }
        }
        throw new IllegalStateException("No top-level comma in light-dark(" + args + ")");
    }
}

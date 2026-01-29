package com.itda.backend.node.generation;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GridLayout(int rows, int cols) {

    private static final Pattern LAYOUT_PATTERN = Pattern.compile("^(\\d+)\\s*[xX]\\s*(\\d+)$");

    public static GridLayout parse(String layout) {
        GridLayout parsed = tryParse(layout);
        if (parsed == null) {
            return new GridLayout(2, 2);
        }
        return parsed;
    }

    public static GridLayout tryParse(String layout) {
        if (layout == null) {
            return null;
        }
        String trimmed = layout.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        Matcher matcher = LAYOUT_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return null;
        }
        int rows = parsePositiveInt(matcher.group(1));
        int cols = parsePositiveInt(matcher.group(2));
        if (rows <= 0 || cols <= 0) {
            return null;
        }
        return new GridLayout(rows, cols);
    }

    public int panelCount() {
        return rows * cols;
    }

    public String normalized() {
        return rows + "x" + cols;
    }

    private static int parsePositiveInt(String value) {
        if (value == null) {
            return -1;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}


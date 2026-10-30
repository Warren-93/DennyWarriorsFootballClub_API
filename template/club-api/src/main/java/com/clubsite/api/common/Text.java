package com.clubsite.api.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/** Small string helpers shared by the feature services. */
public final class Text {

    private Text() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String normalise(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    /** Null-safe, trim- and case-insensitive equality. */
    public static boolean sameText(String left, String right) {
        return Objects.equals(normalise(left), normalise(right));
    }

    /** "Cup Final Report: 3–1 Win!" -> "cup-final-report-3-1-win" */
    public static String slugify(String value) {
        if (value == null) {
            return "";
        }
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}

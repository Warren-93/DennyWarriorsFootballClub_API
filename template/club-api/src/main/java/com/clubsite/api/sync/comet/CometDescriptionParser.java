package com.clubsite.api.sync.comet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Comet's "Home - Away H:A" match descriptions (score omitted for
 * unplayed fixtures). Regex-parsed rather than trusted as structured data.
 */
final class CometDescriptionParser {

    record Parsed(String homeTeam, String awayTeam, Integer homeScore, Integer awayScore) {
        static final Parsed EMPTY = new Parsed(null, null, null, null);
    }

    private static final Pattern PLAYED = Pattern.compile("^(.+?) - (.+?) (\\d+):(\\d+)$");
    // A trailing score with a marker after it, e.g. "3:0*" for a walkover/forfeit.
    private static final Pattern TRAILING_SCORE = Pattern.compile("^(.*?)\\s+\\d+:\\d+\\D*$");
    private static final String TEAM_SEPARATOR = " - ";

    private CometDescriptionParser() {
    }

    static Parsed parse(String description) {
        if (description == null || description.isBlank()) {
            return Parsed.EMPTY;
        }
        String trimmed = description.trim();

        Matcher played = PLAYED.matcher(trimmed);
        if (played.matches()) {
            return new Parsed(played.group(1).trim(), played.group(2).trim(),
                    Integer.parseInt(played.group(3)), Integer.parseInt(played.group(4)));
        }

        int separator = trimmed.indexOf(TEAM_SEPARATOR);
        if (separator <= 0) {
            return Parsed.EMPTY;
        }
        // Unplayed, or a marked score we don't trust as a real result: keep
        // clean team names and no score.
        String away = trimmed.substring(separator + TEAM_SEPARATOR.length()).trim();
        Matcher trailingScore = TRAILING_SCORE.matcher(away);
        return new Parsed(trimmed.substring(0, separator).trim(),
                trailingScore.matches() ? trailingScore.group(1).trim() : away, null, null);
    }
}

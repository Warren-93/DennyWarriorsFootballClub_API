package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import mark.warren93.dev.DennyWarriorsAPI.model.MatchResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps Comet's {@link MatchResult} report row onto the club's own
 * {@link Fixture} shape. Comet has no separate score/team-name fields —
 * both are embedded in matchDescription as "Home - Away H:A" (score omitted
 * for unplayed fixtures), so this is regex-parsed rather than trusted as
 * structured data.
 */
@Component
public class LeagueDataMapper {

    private static final Pattern PLAYED_PATTERN = Pattern.compile("^(.+?) - (.+?) (\\d+):(\\d+)$");
    private static final Pattern TRAILING_SCORE_PATTERN = Pattern.compile("^(.*?)\\s+\\d+:\\d+\\D*$");
    private static final String TEAM_SEPARATOR = " - ";

    public Fixture toFixture(MatchResult match, Fixture existing) {
        Fixture fixture = existing != null ? existing : new Fixture();
        Instant now = Instant.now();

        fixture.setLeagueFixtureId(String.valueOf(match.getMatchId()));
        fixture.setCompetition(match.getName());
        fixture.setSeason(match.getSeason());
        fixture.setKickoffAt(match.getMatchDate() != null ? Instant.ofEpochMilli(match.getMatchDate()) : null);
        fixture.setVenue(match.getFacility());
        fixture.setStatus(match.getMatchStatus());

        applyDescription(fixture, match.getMatchDescription());

        fixture.setLastSyncedAt(now);
        if (fixture.getCreatedAt() == null) {
            fixture.setCreatedAt(now);
        }
        fixture.setUpdatedAt(now);
        return fixture;
    }

    private void applyDescription(Fixture fixture, String description) {
        if (description == null || description.isBlank()) {
            return;
        }

        Matcher played = PLAYED_PATTERN.matcher(description.trim());
        if (played.matches()) {
            fixture.setHomeTeam(played.group(1).trim());
            fixture.setAwayTeam(played.group(2).trim());
            fixture.setHomeScore(Integer.parseInt(played.group(3)));
            fixture.setAwayScore(Integer.parseInt(played.group(4)));
            return;
        }

        int separatorIndex = description.indexOf(TEAM_SEPARATOR);
        if (separatorIndex > 0) {
            fixture.setHomeTeam(description.substring(0, separatorIndex).trim());
            fixture.setAwayTeam(stripTrailingScore(description.substring(separatorIndex + TEAM_SEPARATOR.length()).trim()));
        }
        fixture.setHomeScore(null);
        fixture.setAwayScore(null);
    }

    /**
     * Strips a trailing "H:A" (optionally with a walkover/forfeit marker like
     * "*" after it) from an away-team segment we've already decided not to
     * trust as a real score, so the team name itself still comes out clean.
     */
    private String stripTrailingScore(String awayTeamSegment) {
        Matcher trailingScore = TRAILING_SCORE_PATTERN.matcher(awayTeamSegment);
        return trailingScore.matches() ? trailingScore.group(1).trim() : awayTeamSegment;
    }
}

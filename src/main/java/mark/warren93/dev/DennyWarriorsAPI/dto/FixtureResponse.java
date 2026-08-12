package mark.warren93.dev.DennyWarriorsAPI.dto;

import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record FixtureResponse(
        String id,
        String leagueFixtureId,
        String competition,
        String season,
        String homeTeam,
        String awayTeam,
        Instant kickoffAt,
        String venue,
        String status,
        Integer homeScore,
        Integer awayScore,
        List<Fixture.MatchEvent> matchEvents,
        Instant lastSyncedAt,
        // Club-relative convenience fields for the single-club frontend —
        // derived from the neutral home/away data above using dwfc.club.name.
        String opponent,
        Boolean home,
        Integer goalsFor,
        Integer goalsAgainst,
        Instant date,
        String time) {

    private static final ZoneId KICKOFF_ZONE = ZoneId.of("Europe/London");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static FixtureResponse from(Fixture fixture, String clubName) {
        boolean isHome = clubName != null && clubName.equalsIgnoreCase(fixture.getHomeTeam());
        boolean isAway = clubName != null && clubName.equalsIgnoreCase(fixture.getAwayTeam());

        String opponent = isHome ? fixture.getAwayTeam() : isAway ? fixture.getHomeTeam() : null;
        Boolean home = (isHome || isAway) ? isHome : null;
        Integer goalsFor = isHome ? fixture.getHomeScore() : isAway ? fixture.getAwayScore() : null;
        Integer goalsAgainst = isHome ? fixture.getAwayScore() : isAway ? fixture.getHomeScore() : null;
        String time = fixture.getKickoffAt() != null
                ? TIME_FORMAT.format(fixture.getKickoffAt().atZone(KICKOFF_ZONE))
                : null;

        return new FixtureResponse(
                fixture.getId(),
                fixture.getLeagueFixtureId(),
                fixture.getCompetition(),
                fixture.getSeason(),
                fixture.getHomeTeam(),
                fixture.getAwayTeam(),
                fixture.getKickoffAt(),
                fixture.getVenue(),
                fixture.getStatus(),
                fixture.getHomeScore(),
                fixture.getAwayScore(),
                fixture.getMatchEvents(),
                fixture.getLastSyncedAt(),
                opponent,
                home,
                goalsFor,
                goalsAgainst,
                fixture.getKickoffAt(),
                time);
    }
}

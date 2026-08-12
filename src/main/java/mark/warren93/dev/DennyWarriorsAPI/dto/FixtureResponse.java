package mark.warren93.dev.DennyWarriorsAPI.dto;

import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;

import java.time.Instant;
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
        Instant lastSyncedAt) {

    public static FixtureResponse from(Fixture fixture) {
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
                fixture.getLastSyncedAt());
    }
}

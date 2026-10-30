package com.clubsite.api.matches;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class FixtureDtos {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private FixtureDtos() {
    }

    /** Manual fixture entry. Leave both scores empty until the match is played. */
    public record FixtureRequest(
            @NotBlank @Size(max = 120) String competition,
            @Size(max = 20) String season,
            @NotBlank @Size(max = 120) String homeTeam,
            @NotBlank @Size(max = 120) String awayTeam,
            @NotNull Instant kickoffAt,
            @Size(max = 200) String venue,
            @Size(max = 40) String status,
            @Min(0) Integer homeScore,
            @Min(0) Integer awayScore,
            List<Fixture.MatchEvent> events) {
    }

    /**
     * Neutral fields, plus the club's point of view (home, opponent,
     * scoreFor/Against, outcome) and the kick-off time in the club's zone.
     */
    public record FixtureResponse(
            String id,
            String externalId,
            Fixture.Source source,
            String competition,
            String season,
            String homeTeam,
            String awayTeam,
            Instant kickoffAt,
            String localKickoffTime,
            String venue,
            String status,
            Integer homeScore,
            Integer awayScore,
            List<Fixture.MatchEvent> events,
            Boolean home,
            String opponent,
            Integer scoreFor,
            Integer scoreAgainst,
            ClubPerspective.Outcome outcome) {

        public static FixtureResponse from(Fixture f, ClubPerspective perspective, ZoneId zone) {
            ClubPerspective.View view = perspective.of(f);
            return new FixtureResponse(
                    f.getId(),
                    f.getExternalId(),
                    f.getSource(),
                    f.getCompetition(),
                    f.getSeason(),
                    f.getHomeTeam(),
                    f.getAwayTeam(),
                    f.getKickoffAt(),
                    f.getKickoffAt() == null ? null : TIME_FORMAT.format(f.getKickoffAt().atZone(zone)),
                    f.getVenue(),
                    f.getStatus(),
                    f.getHomeScore(),
                    f.getAwayScore(),
                    f.getEvents() == null ? List.of() : f.getEvents(),
                    view.home(),
                    view.opponent(),
                    view.scoreFor(),
                    view.scoreAgainst(),
                    view.outcome());
        }
    }
}

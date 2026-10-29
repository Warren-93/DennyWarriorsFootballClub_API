package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.config.ClubConfig;
import mark.warren93.dev.DennyWarriorsAPI.dto.SeasonStatsResponse;
import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import mark.warren93.dev.DennyWarriorsAPI.model.StandingsRow;
import mark.warren93.dev.DennyWarriorsAPI.repository.FixtureRepository;
import mark.warren93.dev.DennyWarriorsAPI.repository.StandingsRowRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Season stats computed from the club's own synced Fixture data (now that
 * LeagueDataMapper parses real scores out of Comet's matchDescription),
 * combined with the club's current standings position.
 */
@Service
public class StatsService {

    private final FixtureRepository fixtureRepository;
    private final StandingsRowRepository standingsRowRepository;
    private final ClubConfig clubConfig;

    public StatsService(
            FixtureRepository fixtureRepository,
            StandingsRowRepository standingsRowRepository,
            ClubConfig clubConfig) {
        this.fixtureRepository = fixtureRepository;
        this.standingsRowRepository = standingsRowRepository;
        this.clubConfig = clubConfig;
    }

    public SeasonStatsResponse getSeasonStats() {
        List<Fixture> allFixtures = fixtureRepository.findAll();

        String season = inferCurrentSeason(allFixtures).orElse(null);

        List<Fixture> seasonResults = allFixtures.stream()
                .filter(f -> season == null || Objects.equals(normalise(f.getSeason()), normalise(season)))
                .filter(this::isClubFixture)
                .filter(f -> clubGoalsFor(f) != null && clubGoalsAgainst(f) != null)
                .toList();

        int played = seasonResults.size();
        int won = 0;
        int drawn = 0;
        int lost = 0;
        int goalsFor = 0;
        int goalsAgainst = 0;

        for (Fixture f : seasonResults) {
            int gf = clubGoalsFor(f);
            int ga = clubGoalsAgainst(f);
            goalsFor += gf;
            goalsAgainst += ga;
            if (gf > ga) won++;
            else if (gf == ga) drawn++;
            else lost++;
        }

        int points = (won * 3) + drawn;
        Integer leaguePosition = standingsRowRepository.findAll().stream()
                .filter(row -> clubConfig.isClub(row.getClubId(), row.getClub()))
                .map(StandingsRow::getPosition)
                .findFirst()
                .orElse(null);

        return new SeasonStatsResponse(season, played, won, drawn, lost, goalsFor, goalsAgainst, points, leaguePosition);
    }

    private boolean isClubFixture(Fixture fixture) {
        return clubConfig.isHomeTeam(fixture) || clubConfig.isAwayTeam(fixture);
    }

    private Integer clubGoalsFor(Fixture fixture) {
        if (clubConfig.isHomeTeam(fixture)) return fixture.getHomeScore();
        if (clubConfig.isAwayTeam(fixture)) return fixture.getAwayScore();
        return null;
    }

    private Integer clubGoalsAgainst(Fixture fixture) {
        if (clubConfig.isHomeTeam(fixture)) return fixture.getAwayScore();
        if (clubConfig.isAwayTeam(fixture)) return fixture.getHomeScore();
        return null;
    }

    private Optional<String> inferCurrentSeason(List<Fixture> fixtures) {
        return fixtures.stream()
                .filter(f -> f.getSeason() != null && !f.getSeason().isBlank())
                .sorted(Comparator.comparing(Fixture::getKickoffAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(Fixture::getSeason)
                .findFirst();
    }

    private String normalise(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}

package com.clubsite.api.matches;

import com.clubsite.api.common.Text;
import com.clubsite.api.config.ClubProperties;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Season summary computed from the club's own played fixtures, plus its
 * current league position. Points use club.scoring.*, so the numbers are
 * right for sports that don't award 3/1/0.
 */
@Service
public class StatsService {

    public record SeasonStats(
            String season,
            int played,
            int won,
            int drawn,
            int lost,
            int scoreFor,
            int scoreAgainst,
            int points,
            Integer leaguePosition) {
    }

    private final FixtureService fixtureService;
    private final StandingsRowRepository standingsRepository;
    private final ClubPerspective perspective;
    private final ClubProperties.Scoring scoring;

    public StatsService(
            FixtureService fixtureService,
            StandingsRowRepository standingsRepository,
            ClubPerspective perspective,
            ClubProperties properties) {
        this.fixtureService = fixtureService;
        this.standingsRepository = standingsRepository;
        this.perspective = perspective;
        this.scoring = properties.scoring();
    }

    /** @param season defaults to the season of the most recent fixture */
    public SeasonStats seasonStats(String season) {
        String effectiveSeason = Text.isBlank(season) ? fixtureService.seasons().stream().findFirst().orElse(null) : season;
        List<Fixture> fixtures = fixtureService.list(effectiveSeason, null, null, null);

        Integer leaguePosition = standingsRepository.findAll().stream()
                .filter(row -> perspective.isClub(row.getTeam()))
                .map(StandingsRow::getPosition)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return calculate(effectiveSeason, fixtures, perspective, scoring, leaguePosition);
    }

    /** Pure function, kept static so it can be unit-tested without MongoDB. */
    static SeasonStats calculate(String season, List<Fixture> fixtures, ClubPerspective perspective,
                                 ClubProperties.Scoring scoring, Integer leaguePosition) {
        int won = 0, drawn = 0, lost = 0, scoreFor = 0, scoreAgainst = 0;
        for (Fixture fixture : fixtures) {
            ClubPerspective.View view = perspective.of(fixture);
            if (view.outcome() == null) {
                continue;
            }
            scoreFor += view.scoreFor();
            scoreAgainst += view.scoreAgainst();
            switch (view.outcome()) {
                case W -> won++;
                case D -> drawn++;
                case L -> lost++;
            }
        }
        int points = won * scoring.pointsForWin() + drawn * scoring.pointsForDraw() + lost * scoring.pointsForLoss();
        return new SeasonStats(season, won + drawn + lost, won, drawn, lost, scoreFor, scoreAgainst, points, leaguePosition);
    }
}

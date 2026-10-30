package com.clubsite.api.matches;

import com.clubsite.api.common.Text;
import com.clubsite.api.config.ClubProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Turns neutral "home vs away" fixture data into the club's point of view.
 * The one place that knows how the club's name is matched, so the fixture
 * response, the stats and the league table all agree.
 */
@Component
public class ClubPerspective {

    public enum Outcome { W, D, L }

    /** The club's view of one fixture. Every field is null when the club didn't play in it. */
    public record View(Boolean home, String opponent, Integer scoreFor, Integer scoreAgainst, Outcome outcome) {
    }

    private final String clubName;

    @Autowired
    public ClubPerspective(ClubProperties properties) {
        this(properties.name());
    }

    public ClubPerspective(String clubName) {
        this.clubName = clubName;
    }

    public String clubName() {
        return clubName;
    }

    public boolean isClub(String teamName) {
        return !Text.isBlank(teamName) && Text.sameText(clubName, teamName);
    }

    public boolean involvesClub(Fixture fixture) {
        return isClub(fixture.getHomeTeam()) || isClub(fixture.getAwayTeam());
    }

    public View of(Fixture fixture) {
        boolean home = isClub(fixture.getHomeTeam());
        boolean away = !home && isClub(fixture.getAwayTeam());
        if (!home && !away) {
            return new View(null, null, null, null, null);
        }

        Integer scoreFor = home ? fixture.getHomeScore() : fixture.getAwayScore();
        Integer scoreAgainst = home ? fixture.getAwayScore() : fixture.getHomeScore();
        Outcome outcome = null;
        if (scoreFor != null && scoreAgainst != null) {
            outcome = scoreFor > scoreAgainst ? Outcome.W : scoreFor.equals(scoreAgainst) ? Outcome.D : Outcome.L;
        }
        return new View(home, home ? fixture.getAwayTeam() : fixture.getHomeTeam(), scoreFor, scoreAgainst, outcome);
    }
}

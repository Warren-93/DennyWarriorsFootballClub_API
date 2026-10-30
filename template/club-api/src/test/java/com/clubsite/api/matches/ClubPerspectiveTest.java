package com.clubsite.api.matches;

import com.clubsite.api.matches.ClubPerspective.Outcome;
import com.clubsite.api.matches.FixtureDtos.FixtureResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ClubPerspectiveTest {

    private final ClubPerspective perspective = new ClubPerspective("Example FC");

    static Fixture fixture(String home, String away, Integer homeScore, Integer awayScore) {
        Fixture f = new Fixture();
        f.setHomeTeam(home);
        f.setAwayTeam(away);
        f.setHomeScore(homeScore);
        f.setAwayScore(awayScore);
        f.setKickoffAt(Instant.parse("2026-08-20T14:00:00Z"));
        return f;
    }

    @Test
    void clubAsHomeTeam() {
        var view = perspective.of(fixture("Example FC", "Rivals United", 3, 0));

        assertThat(view.home()).isTrue();
        assertThat(view.opponent()).isEqualTo("Rivals United");
        assertThat(view.scoreFor()).isEqualTo(3);
        assertThat(view.scoreAgainst()).isZero();
        assertThat(view.outcome()).isEqualTo(Outcome.W);
    }

    @Test
    void clubAsAwayTeam() {
        var view = perspective.of(fixture("Rivals United", "Example FC", 2, 0));

        assertThat(view.home()).isFalse();
        assertThat(view.opponent()).isEqualTo("Rivals United");
        assertThat(view.scoreFor()).isZero();
        assertThat(view.scoreAgainst()).isEqualTo(2);
        assertThat(view.outcome()).isEqualTo(Outcome.L);
    }

    @Test
    void drawAndUnplayed() {
        assertThat(perspective.of(fixture("Example FC", "Rivals United", 1, 1)).outcome()).isEqualTo(Outcome.D);
        assertThat(perspective.of(fixture("Example FC", "Rivals United", null, null)).outcome()).isNull();
    }

    @Test
    void nameMatchIgnoresCaseAndSpacing() {
        assertThat(perspective.of(fixture("  example fc ", "Rivals United", 1, 3)).home()).isTrue();
    }

    @Test
    void fixtureNotInvolvingTheClubHasNoClubView() {
        var view = perspective.of(fixture("Team A", "Team B", 1, 1));

        assertThat(view.home()).isNull();
        assertThat(view.opponent()).isNull();
        assertThat(view.outcome()).isNull();
    }

    @Test
    void responseShowsKickoffInTheClubsTimezone() {
        Fixture f = fixture("Example FC", "Rivals United", null, null);

        // 14:00 UTC in August: 15:00 in London (BST), 16:00 in Paris (CEST).
        assertThat(FixtureResponse.from(f, perspective, ZoneId.of("Europe/London")).localKickoffTime()).isEqualTo("15:00");
        assertThat(FixtureResponse.from(f, perspective, ZoneId.of("Europe/Paris")).localKickoffTime()).isEqualTo("16:00");
    }
}

package mark.warren93.dev.DennyWarriorsAPI.dto;

import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FixtureResponseTest {

    private Fixture fixture(String home, String away, Integer homeScore, Integer awayScore) {
        Fixture f = new Fixture();
        f.setHomeTeam(home);
        f.setAwayTeam(away);
        f.setHomeScore(homeScore);
        f.setAwayScore(awayScore);
        f.setKickoffAt(Instant.parse("2026-08-20T15:00:00Z"));
        return f;
    }

    @Test
    void clubAsHomeTeam() {
        FixtureResponse response = FixtureResponse.from(fixture("Denny Warriors", "Edinburgh Thistle", 3, 0), "Denny Warriors");

        assertThat(response.home()).isTrue();
        assertThat(response.opponent()).isEqualTo("Edinburgh Thistle");
        assertThat(response.goalsFor()).isEqualTo(3);
        assertThat(response.goalsAgainst()).isEqualTo(0);
    }

    @Test
    void clubAsAwayTeam() {
        FixtureResponse response = FixtureResponse.from(fixture("Dundee Discovery", "Denny Warriors", 0, 2), "Denny Warriors");

        assertThat(response.home()).isFalse();
        assertThat(response.opponent()).isEqualTo("Dundee Discovery");
        assertThat(response.goalsFor()).isEqualTo(2);
        assertThat(response.goalsAgainst()).isEqualTo(0);
    }

    @Test
    void clubNameMatchIsCaseInsensitive() {
        FixtureResponse response = FixtureResponse.from(fixture("denny warriors", "Rangers FFIT", 1, 3), "Denny Warriors");

        assertThat(response.home()).isTrue();
        assertThat(response.opponent()).isEqualTo("Rangers FFIT");
    }

    @Test
    void neitherTeamMatchesClubNameLeavesConvenienceFieldsNull() {
        FixtureResponse response = FixtureResponse.from(fixture("Team A", "Team B", 1, 1), "Denny Warriors");

        assertThat(response.home()).isNull();
        assertThat(response.opponent()).isNull();
        assertThat(response.goalsFor()).isNull();
        assertThat(response.goalsAgainst()).isNull();
    }

    @Test
    void timeIsFormattedInLondonTimeZone() {
        FixtureResponse response = FixtureResponse.from(fixture("Denny Warriors", "Edinburgh Thistle", null, null), "Denny Warriors");

        // 15:00 UTC in August is BST (UTC+1) -> 16:00 local
        assertThat(response.time()).isEqualTo("16:00");
    }
}

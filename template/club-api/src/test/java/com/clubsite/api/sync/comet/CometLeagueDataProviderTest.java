package com.clubsite.api.sync.comet;

import com.clubsite.api.sync.LeagueDataProvider.ExternalFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Cases taken from real Comet data. */
class CometLeagueDataProviderTest {

    private ExternalFixture map(String description, String status) {
        return CometLeagueDataProvider.toExternal(new CometPayloads.Match(
                42L, description, status, 1755442800000L, "Premier League 25/26", "2025/2026", "Home Park"));
    }

    @Test
    void parsesPlayedScoreAndTeamNames() {
        ExternalFixture f = map("Example FC - Rivals United 3:0", "PLAYED");

        assertThat(f.externalId()).isEqualTo("42");
        assertThat(f.homeTeam()).isEqualTo("Example FC");
        assertThat(f.awayTeam()).isEqualTo("Rivals United");
        assertThat(f.homeScore()).isEqualTo(3);
        assertThat(f.awayScore()).isZero();
        assertThat(f.competition()).isEqualTo("Premier League 25/26");
        assertThat(f.kickoffAt()).isEqualTo(Instant.ofEpochMilli(1755442800000L));
    }

    @Test
    void unplayedFixtureHasTeamNamesButNoScore() {
        ExternalFixture f = map("Example FC - Rivals United", "SCHEDULED");

        assertThat(f.homeTeam()).isEqualTo("Example FC");
        assertThat(f.awayTeam()).isEqualTo("Rivals United");
        assertThat(f.homeScore()).isNull();
        assertThat(f.awayScore()).isNull();
    }

    @Test
    void walkoverMarkerDropsTheScoreButKeepsACleanTeamName() {
        // A trailing "*" marks walkovers/forfeits: the score isn't a real result.
        ExternalFixture f = map("Example FC - Rivals United 3:0*", "PLAYED");

        assertThat(f.awayTeam()).isEqualTo("Rivals United");
        assertThat(f.homeScore()).isNull();
        assertThat(f.awayScore()).isNull();
    }

    @Test
    void hyphenatedTeamNamesSurvive() {
        ExternalFixture f = map("Stoke-on-Trent Rovers - Example FC 1:2", "PLAYED");

        assertThat(f.homeTeam()).isEqualTo("Stoke-on-Trent Rovers");
        assertThat(f.awayTeam()).isEqualTo("Example FC");
    }

    @Test
    void blankDescriptionGivesNoTeams() {
        ExternalFixture f = map("", "PLAYED");

        assertThat(f.homeTeam()).isNull();
        assertThat(f.awayTeam()).isNull();
    }
}

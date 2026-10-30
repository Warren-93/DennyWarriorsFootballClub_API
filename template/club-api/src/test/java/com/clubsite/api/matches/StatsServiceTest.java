package com.clubsite.api.matches;

import com.clubsite.api.config.ClubProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.clubsite.api.matches.ClubPerspectiveTest.fixture;
import static org.assertj.core.api.Assertions.assertThat;

class StatsServiceTest {

    private final ClubPerspective perspective = new ClubPerspective("Example FC");

    private final List<Fixture> season = List.of(
            fixture("Example FC", "A", 3, 1),       // W
            fixture("B", "Example FC", 0, 2),       // W (away)
            fixture("Example FC", "C", 1, 1),       // D
            fixture("D", "Example FC", 4, 0),       // L
            fixture("Example FC", "E", null, null), // unplayed: ignored
            fixture("F", "G", 2, 2));               // not our match: ignored

    @Test
    void footballScoring() {
        var stats = StatsService.calculate("2025/26", season, perspective, new ClubProperties.Scoring(3, 1, 0), 2);

        assertThat(stats.played()).isEqualTo(4);
        assertThat(stats.won()).isEqualTo(2);
        assertThat(stats.drawn()).isEqualTo(1);
        assertThat(stats.lost()).isEqualTo(1);
        assertThat(stats.scoreFor()).isEqualTo(6);
        assertThat(stats.scoreAgainst()).isEqualTo(6);
        assertThat(stats.points()).isEqualTo(7);
        assertThat(stats.leaguePosition()).isEqualTo(2);
    }

    @Test
    void pointsFollowConfiguredScoring() {
        // e.g. rugby union without bonus points: 4 for a win, 2 for a draw.
        var stats = StatsService.calculate("2025/26", season, perspective, new ClubProperties.Scoring(4, 2, 0), null);

        assertThat(stats.points()).isEqualTo(10);
    }
}

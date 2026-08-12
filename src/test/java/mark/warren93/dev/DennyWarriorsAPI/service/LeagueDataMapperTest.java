package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import mark.warren93.dev.DennyWarriorsAPI.model.MatchResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LeagueDataMapperTest {

    private final LeagueDataMapper mapper = new LeagueDataMapper();

    private MatchResult match(String description, String status) {
        MatchResult match = new MatchResult();
        match.setMatchId(42L);
        match.setMatchDescription(description);
        match.setMatchStatus(status);
        match.setSeason("2025/2026");
        match.setName("Appin Warriors Premier League 25/26");
        match.setFacility("Ochilview Park");
        match.setMatchDate(1755442800000L);
        return match;
    }

    @Test
    void parsesPlayedScoreAndTeamNames() {
        Fixture fixture = mapper.toFixture(match("Denny Warriors - Edinburgh Thistle 3:0", "PLAYED"), null);

        assertThat(fixture.getHomeTeam()).isEqualTo("Denny Warriors");
        assertThat(fixture.getAwayTeam()).isEqualTo("Edinburgh Thistle");
        assertThat(fixture.getHomeScore()).isEqualTo(3);
        assertThat(fixture.getAwayScore()).isEqualTo(0);
        assertThat(fixture.getLeagueFixtureId()).isEqualTo("42");
        assertThat(fixture.getCompetition()).isEqualTo("Appin Warriors Premier League 25/26");
        assertThat(fixture.getStatus()).isEqualTo("PLAYED");
    }

    @Test
    void unplayedFixtureHasNoScoreButKeepsTeamNames() {
        Fixture fixture = mapper.toFixture(match("Denny Warriors - Edinburgh Thistle", "SCHEDULED"), null);

        assertThat(fixture.getHomeTeam()).isEqualTo("Denny Warriors");
        assertThat(fixture.getAwayTeam()).isEqualTo("Edinburgh Thistle");
        assertThat(fixture.getHomeScore()).isNull();
        assertThat(fixture.getAwayScore()).isNull();
    }

    @Test
    void walkoverMarkerFallsBackToNoScoreWithCleanTeamName() {
        // Real Comet data includes a trailing "*" on some played-score descriptions
        // (e.g. walkovers/forfeits) — the score there isn't a reliable "H:A" result,
        // so it should be dropped, but the team name must still come out clean.
        Fixture fixture = mapper.toFixture(match("Denny Warriors - Glasgow Wellington 3:0*", "PLAYED"), null);

        assertThat(fixture.getHomeTeam()).isEqualTo("Denny Warriors");
        assertThat(fixture.getAwayTeam()).isEqualTo("Glasgow Wellington");
        assertThat(fixture.getHomeScore()).isNull();
        assertThat(fixture.getAwayScore()).isNull();
    }

    @Test
    void blankDescriptionLeavesExistingTeamNamesAndScoresUntouched() {
        Fixture existing = new Fixture();
        existing.setHomeTeam("Denny Warriors");
        existing.setAwayTeam("Edinburgh Thistle");
        existing.setHomeScore(3);
        existing.setAwayScore(0);

        Fixture fixture = mapper.toFixture(match("", "PLAYED"), existing);

        assertThat(fixture.getHomeTeam()).isEqualTo("Denny Warriors");
        assertThat(fixture.getAwayTeam()).isEqualTo("Edinburgh Thistle");
        assertThat(fixture.getHomeScore()).isEqualTo(3);
        assertThat(fixture.getAwayScore()).isEqualTo(0);
    }

    @Test
    void reusesExistingDocumentIdAndCreatedAtOnUpdate() {
        Fixture existing = new Fixture();
        existing.setId("mongo-id-1");
        Instant originalCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
        existing.setCreatedAt(originalCreatedAt);

        Fixture fixture = mapper.toFixture(match("Denny Warriors - Edinburgh Thistle 3:0", "PLAYED"), existing);

        assertThat(fixture.getId()).isEqualTo("mongo-id-1");
        assertThat(fixture.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(fixture.getUpdatedAt()).isNotNull();
        assertThat(fixture.getLastSyncedAt()).isNotNull();
    }

    @Test
    void convertsEpochMillisKickoffToInstant() {
        Fixture fixture = mapper.toFixture(match("Denny Warriors - Edinburgh Thistle 3:0", "PLAYED"), null);

        assertThat(fixture.getKickoffAt()).isEqualTo(Instant.ofEpochMilli(1755442800000L));
    }
}

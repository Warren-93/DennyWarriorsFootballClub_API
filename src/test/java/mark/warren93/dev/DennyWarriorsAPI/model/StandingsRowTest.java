package mark.warren93.dev.DennyWarriorsAPI.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StandingsRowTest {

    /** One row of Comet's "Competition Standings" report, trimmed to the columns we read. */
    private static final String COMET_ROW = """
            {
              "id": 32660305,
              "name": "Appin Warriors Premier League 25/26",
              "season": "2025/2026",
              "competitionType": "Warriors FA League Competition",
              "status": "INACTIVE",
              "clubId": 185523,
              "club": "Camelon Juniors Warriors",
              "position": 5,
              "matches": 20,
              "wins": 13,
              "draws": 1,
              "losses": 6,
              "goalsFor": 48,
              "goalsAgainst": 34,
              "goalDifference": 14,
              "points": 40,
              "#result_id#": "5"
            }
            """;

    @Test
    void cometIdColumnBecomesCompetitionIdNotMongoId() throws Exception {
        StandingsRow row = new ObjectMapper().readValue(COMET_ROW, StandingsRow.class);

        assertThat(row.getId()).isNull();
        assertThat(row.getCompetitionId()).isEqualTo(32660305L);
        assertThat(row.getCompetition()).isEqualTo("Appin Warriors Premier League 25/26");
        assertThat(row.getSeason()).isEqualTo("2025/2026");
        assertThat(row.getStatus()).isEqualTo("INACTIVE");
        assertThat(row.getClubId()).isEqualTo(185523L);
        assertThat(row.getClub()).isEqualTo("Camelon Juniors Warriors");
        assertThat(row.getPosition()).isEqualTo(5);
        assertThat(row.getPoints()).isEqualTo(40);
    }
}

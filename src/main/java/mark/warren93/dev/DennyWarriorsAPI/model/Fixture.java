package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fixtures")
public class Fixture {

    @Id
    private String id;
    private String leagueFixtureId;
    private String competition;
    private String season;
    private String homeTeam;
    private String awayTeam;
    private Instant kickoffAt;
    private String venue;
    /** Passed through as-is from the league API — not a hard enum, its vocabulary isn't fully known. */
    private String status;
    private Integer homeScore;
    private Integer awayScore;
    private List<MatchEvent> matchEvents = new ArrayList<>();
    private Instant lastSyncedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchEvent {
        private Integer minute;
        private String type;
        private String player;
        private String detail;
    }
}

package com.clubsite.api.matches;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A match, stored neutrally as home vs away; the club-relative view
 * (opponent, home/away, W/D/L) is derived by ClubPerspective. A fixture
 * becomes a "result" once it has a score: there is no separate collection.
 */
@Data
@NoArgsConstructor
@Document(collection = "fixtures")
public class Fixture {

    public enum Source {
        /** Entered in the admin panel. */
        MANUAL,
        /** Upserted by SyncService from a LeagueDataProvider; edits are overwritten on the next sync. */
        SYNC
    }

    @Id
    private String id;
    /** The league system's id for this match; null for manual fixtures. */
    @Indexed(unique = true, sparse = true)
    private String externalId;
    private Source source;
    private String competition;
    private String season;
    private String homeTeam;
    private String awayTeam;
    @Indexed
    private Instant kickoffAt;
    private String venue;
    /** Passed through as-is from the league; its vocabulary varies by provider. */
    private String status;
    private Integer homeScore;
    private Integer awayScore;
    private List<MatchEvent> events = new ArrayList<>();
    private Instant lastSyncedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchEvent {
        private Integer minute;
        /** Free text: "GOAL", "YELLOW_CARD", "TRY"... whatever suits the sport. */
        private String type;
        private String player;
        private String detail;
    }

    public boolean hasScore() {
        return homeScore != null && awayScore != null;
    }
}

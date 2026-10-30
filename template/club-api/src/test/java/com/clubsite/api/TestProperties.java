package com.clubsite.api;

import com.clubsite.api.config.ClubProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/** Builds ClubProperties for plain unit tests, with the same defaults as production. */
public final class TestProperties {

    public static final String CLUB = "Example FC";
    public static final String SECRET = "a".repeat(32);

    private TestProperties() {
    }

    public static ClubProperties defaults() {
        return withScoring(new ClubProperties.Scoring(3, 1, 0));
    }

    public static ClubProperties withScoring(ClubProperties.Scoring scoring) {
        return new ClubProperties(
                CLUB,
                ZoneId.of("Europe/London"),
                scoring,
                new ClubProperties.Security(List.of("http://localhost:5173"), jwt(), true),
                new ClubProperties.Admin(false, "admin", null),
                new ClubProperties.Media(DataSize.ofMegabytes(5), Set.of("image/jpeg", "image/png", "image/webp", "image/gif")),
                new ClubProperties.League("manual", false, Duration.ofSeconds(10), Duration.ofDays(1), Duration.ofDays(1),
                        new ClubProperties.Comet(null, null)),
                new ClubProperties.YouTube(null));
    }

    public static ClubProperties.Jwt jwt() {
        return new ClubProperties.Jwt(SECRET, Duration.ofHours(1), Duration.ofDays(7), "club-api");
    }
}

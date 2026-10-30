package com.clubsite.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Everything that differs between one club's deployment and another's lives
 * under the {@code club.*} prefix. application.properties maps these onto
 * friendly environment variable names (CLUB_NAME, JWT_SECRET, ...); see
 * .env.example for the full list.
 *
 * Nested groups use @DefaultValue so a fresh project starts with sensible
 * values and only has to set what is genuinely club-specific.
 */
@Validated
@ConfigurationProperties(prefix = "club")
public record ClubProperties(
        /*
         * The club's name exactly as the league data spells it (matched
         * case-insensitively). Used to work out home/away, opponent and the
         * club's own row in the table from neutral home/away fixture data.
         */
        @NotBlank String name,
        /* Kick-off times are rendered in this zone. */
        @DefaultValue("Europe/London") ZoneId timezone,
        @Valid @DefaultValue Scoring scoring,
        @Valid @DefaultValue Security security,
        @Valid @DefaultValue Admin admin,
        @Valid @DefaultValue Media media,
        @Valid @DefaultValue League league,
        @Valid @DefaultValue YouTube youtube) {

    /** League points per outcome — e.g. 3/1/0 for football, 4/2/0 for rugby union before bonus points. */
    public record Scoring(
            @DefaultValue("3") int pointsForWin,
            @DefaultValue("1") int pointsForDraw,
            @DefaultValue("0") int pointsForLoss) {
    }

    public record Security(
            @DefaultValue("http://localhost:5173") List<String> allowedOrigins,
            @Valid @DefaultValue Jwt jwt,
            @DefaultValue("true") boolean loginRateLimitEnabled) {
    }

    public record Jwt(
            /* Required, at least 32 bytes. JwtService refuses to start without it. */
            String secret,
            @DefaultValue("1h") Duration accessTokenTtl,
            @DefaultValue("7d") Duration refreshTokenTtl,
            @DefaultValue("club-api") String issuer) {
    }

    public record Admin(
            /* Creates the first SUPER_ADMIN when the users collection is empty. */
            @DefaultValue("true") boolean bootstrapEnabled,
            @DefaultValue("admin") String bootstrapUsername,
            String bootstrapPassword) {
    }

    public record Media(
            @DefaultValue("5MB") DataSize maxFileSize,
            @DefaultValue({"image/jpeg", "image/png", "image/webp", "image/gif"}) Set<String> allowedContentTypes) {
    }

    public record League(
            /*
             * Where fixtures and standings come from: "manual" (entered in the
             * admin panel) or "comet" (synced from the Comet report API).
             * Add another LeagueDataProvider to support a different league.
             */
            @DefaultValue("manual") String provider,
            /* Kill switch for the scheduled sync. Manual triggers still work. */
            @DefaultValue("true") boolean syncEnabled,
            @DefaultValue("10s") Duration initialDelay,
            /* Seed interval used until an admin changes it (stored in Mongo after that). */
            @DefaultValue("1d") Duration defaultInterval,
            @DefaultValue("1d") Duration minInterval,
            @Valid @DefaultValue Comet comet) {
    }

    /* Comet report URLs carry an API_KEY parameter: environment variables only, never committed. */
    public record Comet(String fixturesUrl, String standingsUrl) {
    }

    /* Leave the channel id blank to switch the videos endpoint off (it returns an empty list). */
    public record YouTube(String channelId) {
    }
}

package com.clubsite.api.sync;

import java.time.Instant;
import java.util.List;

/**
 * The seam between this API and wherever a league publishes its data.
 * Implement it once per league system (Comet, FA Full-Time, a CSV feed, a
 * scraper...) and select it with club.league.provider. SyncService does the
 * scheduling, retries, upserts and logging; a provider only fetches and maps.
 *
 * Exactly one provider bean must exist. Annotate an implementation with
 * {@code @ConditionalOnProperty(name = "club.league.provider", havingValue = "<id>")}.
 */
public interface LeagueDataProvider {

    /** Shown in the admin panel's sync settings. */
    String id();

    /** False for "manual": SyncService then never schedules or runs a sync. */
    boolean isRemote();

    List<ExternalFixture> fetchFixtures();

    /** An empty list means "no data this time", never "the table is empty"; existing rows are kept. */
    List<ExternalStandingsRow> fetchStandings();

    /**
     * A fixture as the league reports it. Leave homeTeam/awayTeam null when
     * the source didn't include them: the stored names are then kept rather
     * than wiped. Scores are null until the match has a trustworthy result.
     */
    record ExternalFixture(
            String externalId,
            String competition,
            String season,
            String homeTeam,
            String awayTeam,
            Instant kickoffAt,
            String venue,
            String status,
            Integer homeScore,
            Integer awayScore) {
    }

    record ExternalStandingsRow(
            Integer position,
            String team,
            Integer played,
            Integer won,
            Integer drawn,
            Integer lost,
            Integer scoreFor,
            Integer scoreAgainst,
            Integer scoreDifference,
            Integer points) {
    }
}

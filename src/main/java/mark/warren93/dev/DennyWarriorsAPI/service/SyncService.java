package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import mark.warren93.dev.DennyWarriorsAPI.model.MatchResult;
import mark.warren93.dev.DennyWarriorsAPI.model.StandingsRow;
import mark.warren93.dev.DennyWarriorsAPI.model.SyncLog;
import mark.warren93.dev.DennyWarriorsAPI.repository.FixtureRepository;
import mark.warren93.dev.DennyWarriorsAPI.repository.StandingsRowRepository;
import mark.warren93.dev.DennyWarriorsAPI.repository.SyncLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Pulls fixtures + standings from the league API on a schedule (or on
 * demand) and upserts them into MongoDB. The public API only ever reads
 * from MongoDB — this is the one place that talks to Comet.
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_BASE_MS = 500;

    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_MANUAL = "MANUAL";

    private final LeagueApiClient leagueApiClient;
    private final LeagueDataMapper mapper;
    private final FixtureRepository fixtureRepository;
    private final StandingsRowRepository standingsRowRepository;
    private final SyncLogRepository syncLogRepository;

    @Value("${league.sync.enabled:true}")
    private boolean syncEnabled;

    public SyncService(
            LeagueApiClient leagueApiClient,
            LeagueDataMapper mapper,
            FixtureRepository fixtureRepository,
            StandingsRowRepository standingsRowRepository,
            SyncLogRepository syncLogRepository) {
        this.leagueApiClient = leagueApiClient;
        this.mapper = mapper;
        this.fixtureRepository = fixtureRepository;
        this.standingsRowRepository = standingsRowRepository;
        this.syncLogRepository = syncLogRepository;
    }

    @Scheduled(
            initialDelayString = "${league.sync.initial-delay-ms:10000}",
            fixedDelayString = "${league.sync.interval-ms:900000}")
    public void runScheduledSync() {
        if (!syncEnabled) {
            return;
        }
        sync(TRIGGER_SCHEDULED);
    }

    public SyncLog sync(String trigger) {
        SyncLog syncLog = new SyncLog();
        syncLog.setStartedAt(Instant.now());
        syncLog.setTrigger(trigger);

        try {
            List<MatchResult> matches = withRetry("fetchMatches", leagueApiClient::fetchMatches);
            int upserted = upsertFixtures(matches);

            List<StandingsRow> standings = withRetry("fetchStandings", leagueApiClient::fetchStandings);
            replaceStandings(standings);

            syncLog.setStatus("SUCCESS");
            syncLog.setRecordsProcessed(matches.size());
            syncLog.setRecordsUpserted(upserted);
        } catch (Exception ex) {
            log.error("League sync failed (trigger={})", trigger, ex);
            syncLog.setStatus("FAILED");
            syncLog.setErrorMessage(ex.getMessage());
        }

        syncLog.setFinishedAt(Instant.now());
        return syncLogRepository.save(syncLog);
    }

    private int upsertFixtures(List<MatchResult> matches) {
        int upserted = 0;
        for (MatchResult match : matches) {
            if (match.getMatchId() == null) {
                continue;
            }
            String leagueFixtureId = String.valueOf(match.getMatchId());
            Fixture existing = fixtureRepository.findByLeagueFixtureId(leagueFixtureId).orElse(null);
            Fixture fixture = mapper.toFixture(match, existing);
            fixtureRepository.save(fixture);
            upserted++;
        }
        return upserted;
    }

    private void replaceStandings(List<StandingsRow> standings) {
        if (standings.isEmpty()) {
            // A transient empty response shouldn't wipe out otherwise-good data —
            // same "leave existing data untouched" resilience as a failed fetch.
            log.warn("Standings fetch returned no rows; leaving existing standings collection untouched");
            return;
        }
        standingsRowRepository.deleteAll();
        standingsRowRepository.saveAll(standings);
    }

    private <T> T withRetry(String operation, java.util.function.Supplier<T> action) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException ex) {
                lastFailure = ex;
                log.warn("{} failed on attempt {}/{}: {}", operation, attempt, MAX_ATTEMPTS, ex.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleep(BACKOFF_BASE_MS * attempt);
                }
            }
        }
        throw lastFailure;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Sync retry interrupted", ex);
        }
    }
}

package mark.warren93.dev.DennyWarriorsAPI.service;

import jakarta.annotation.PostConstruct;
import mark.warren93.dev.DennyWarriorsAPI.exception.InvalidSyncSettingsException;
import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import mark.warren93.dev.DennyWarriorsAPI.model.MatchResult;
import mark.warren93.dev.DennyWarriorsAPI.model.StandingsRow;
import mark.warren93.dev.DennyWarriorsAPI.model.SyncLog;
import mark.warren93.dev.DennyWarriorsAPI.model.SyncSettings;
import mark.warren93.dev.DennyWarriorsAPI.repository.FixtureRepository;
import mark.warren93.dev.DennyWarriorsAPI.repository.StandingsRowRepository;
import mark.warren93.dev.DennyWarriorsAPI.repository.SyncLogRepository;
import mark.warren93.dev.DennyWarriorsAPI.repository.SyncSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pulls fixtures + standings from the league API on a schedule (or on
 * demand) and upserts them into MongoDB. The public API only ever reads
 * from MongoDB — this is the one place that talks to Comet.
 *
 * The scheduled interval is stored in Mongo (SyncSettings) rather than
 * fixed via a Spring @Scheduled property, so it can be changed live from
 * the admin panel — the next run's own Trigger re-reads the current value,
 * no restart needed.
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_BASE_MS = 500;
    // League data (fixtures/tables) only changes weekly, so 1 day is the floor — no
    // reason to poll Comet more often than that.
    private static final long MIN_INTERVAL_MS = 24L * 60 * 60 * 1000;

    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_MANUAL = "MANUAL";

    private final LeagueApiClient leagueApiClient;
    private final LeagueDataMapper mapper;
    private final FixtureRepository fixtureRepository;
    private final StandingsRowRepository standingsRowRepository;
    private final SyncLogRepository syncLogRepository;
    private final SyncSettingsRepository syncSettingsRepository;
    private final TaskScheduler taskScheduler;

    private final AtomicLong currentIntervalMs = new AtomicLong();

    @Value("${league.sync.enabled:true}")
    private boolean syncEnabled;

    @Value("${league.sync.initial-delay-ms:10000}")
    private long initialDelayMs;

    @Value("${league.sync.interval-ms:900000}")
    private long defaultIntervalMs;

    public SyncService(
            LeagueApiClient leagueApiClient,
            LeagueDataMapper mapper,
            FixtureRepository fixtureRepository,
            StandingsRowRepository standingsRowRepository,
            SyncLogRepository syncLogRepository,
            SyncSettingsRepository syncSettingsRepository,
            TaskScheduler taskScheduler) {
        this.leagueApiClient = leagueApiClient;
        this.mapper = mapper;
        this.fixtureRepository = fixtureRepository;
        this.standingsRowRepository = standingsRowRepository;
        this.syncLogRepository = syncLogRepository;
        this.syncSettingsRepository = syncSettingsRepository;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    void init() {
        SyncSettings settings = syncSettingsRepository.findById(SyncSettings.SINGLETON_ID).orElse(null);
        currentIntervalMs.set(settings != null ? settings.getIntervalMs() : defaultIntervalMs);
        taskScheduler.schedule(this::runScheduledSync, new DynamicIntervalTrigger());
    }

    public long getIntervalMs() {
        return currentIntervalMs.get();
    }

    public SyncSettings updateIntervalMs(long intervalMs) {
        if (intervalMs < MIN_INTERVAL_MS) {
            throw new InvalidSyncSettingsException(
                    "Sync interval must be at least " + (MIN_INTERVAL_MS / 86_400_000) + " day(s)");
        }

        SyncSettings settings = syncSettingsRepository.findById(SyncSettings.SINGLETON_ID)
                .orElseGet(() -> {
                    SyncSettings fresh = new SyncSettings();
                    fresh.setId(SyncSettings.SINGLETON_ID);
                    return fresh;
                });
        settings.setIntervalMs(intervalMs);
        settings.setUpdatedAt(Instant.now());
        settings = syncSettingsRepository.save(settings);

        currentIntervalMs.set(intervalMs);
        return settings;
    }

    public void runScheduledSync() {
        if (!syncEnabled) {
            return;
        }
        sync(TRIGGER_SCHEDULED);
    }

    /** Reads the current interval fresh on every scheduling decision, so changes take effect on the next run. */
    private final class DynamicIntervalTrigger implements Trigger {
        @Override
        public Instant nextExecution(TriggerContext triggerContext) {
            Instant lastCompletion = triggerContext.lastCompletion();
            if (lastCompletion == null) {
                return Instant.now().plusMillis(initialDelayMs);
            }
            return lastCompletion.plusMillis(currentIntervalMs.get());
        }
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

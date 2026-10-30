package com.clubsite.api.sync;

import com.clubsite.api.common.ApiException;
import com.clubsite.api.common.Text;
import com.clubsite.api.config.ClubProperties;
import com.clubsite.api.matches.Fixture;
import com.clubsite.api.matches.FixtureRepository;
import com.clubsite.api.matches.StandingsRow;
import com.clubsite.api.matches.StandingsRowRepository;
import com.clubsite.api.sync.LeagueDataProvider.ExternalFixture;
import com.clubsite.api.sync.LeagueDataProvider.ExternalStandingsRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Pulls fixtures and standings from the configured LeagueDataProvider, on a
 * schedule or on demand, and upserts them into MongoDB. Public endpoints only
 * ever read MongoDB, so a league outage never takes the site down.
 *
 * The interval lives in MongoDB (SyncSettings) rather than a fixed
 * {@code @Scheduled} value, so it can be changed live from the admin panel:
 * the trigger re-reads it every time it schedules the next run.
 */
@Service
public class SyncService {

    public record SyncStatus(String provider, boolean remote, boolean scheduledSyncEnabled,
                             long intervalMs, long intervalDays) {
    }

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_BASE_MS = 500;
    private static final long NOT_LOADED = -1;

    private final LeagueDataProvider provider;
    private final FixtureRepository fixtureRepository;
    private final StandingsRowRepository standingsRepository;
    private final SyncLogRepository syncLogRepository;
    private final SyncSettingsRepository syncSettingsRepository;
    private final TaskScheduler taskScheduler;
    private final ClubProperties.League settings;

    private final AtomicLong intervalMs = new AtomicLong(NOT_LOADED);
    // Stops a manual trigger and the scheduled run upserting at the same time.
    private final ReentrantLock running = new ReentrantLock();

    public SyncService(
            LeagueDataProvider provider,
            FixtureRepository fixtureRepository,
            StandingsRowRepository standingsRepository,
            SyncLogRepository syncLogRepository,
            SyncSettingsRepository syncSettingsRepository,
            TaskScheduler taskScheduler,
            ClubProperties properties) {
        this.provider = provider;
        this.fixtureRepository = fixtureRepository;
        this.standingsRepository = standingsRepository;
        this.syncLogRepository = syncLogRepository;
        this.syncSettingsRepository = syncSettingsRepository;
        this.taskScheduler = taskScheduler;
        this.settings = properties.league();
    }

    /**
     * Runs once the app is up rather than in the constructor, and touches no
     * database itself, so startup never blocks on MongoDB.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void scheduleSync() {
        if (!provider.isRemote()) {
            log.info("League provider '{}' has nothing to sync; scheduled sync not started", provider.id());
            return;
        }
        if (!settings.syncEnabled()) {
            log.info("Scheduled league sync disabled (LEAGUE_SYNC_ENABLED=false)");
            return;
        }
        taskScheduler.schedule(() -> sync(SyncLog.Trigger.SCHEDULED), new DynamicIntervalTrigger());
        log.info("Scheduled league sync from provider '{}'", provider.id());
    }

    public SyncStatus status() {
        long ms = getIntervalMs();
        return new SyncStatus(provider.id(), provider.isRemote(), provider.isRemote() && settings.syncEnabled(),
                ms, Duration.ofMillis(ms).toDays());
    }

    public long getIntervalMs() {
        long current = intervalMs.get();
        if (current != NOT_LOADED) {
            return current;
        }
        long stored = syncSettingsRepository.findById(SyncSettings.SINGLETON_ID)
                .map(SyncSettings::getIntervalMs)
                .filter(ms -> ms > 0)
                .orElse(settings.defaultInterval().toMillis());
        intervalMs.compareAndSet(NOT_LOADED, stored);
        return intervalMs.get();
    }

    public SyncStatus updateInterval(Duration interval) {
        if (interval.compareTo(settings.minInterval()) < 0) {
            throw ApiException.badRequest("Sync interval must be at least " + settings.minInterval().toDays() + " day(s)");
        }
        SyncSettings stored = syncSettingsRepository.findById(SyncSettings.SINGLETON_ID).orElseGet(() -> {
            SyncSettings fresh = new SyncSettings();
            fresh.setId(SyncSettings.SINGLETON_ID);
            return fresh;
        });
        stored.setIntervalMs(interval.toMillis());
        stored.setUpdatedAt(Instant.now());
        syncSettingsRepository.save(stored);
        intervalMs.set(interval.toMillis());
        return status();
    }

    public SyncLog sync(SyncLog.Trigger trigger) {
        SyncLog entry = new SyncLog();
        entry.setStartedAt(Instant.now());
        entry.setTrigger(trigger);
        entry.setProvider(provider.id());

        if (!provider.isRemote()) {
            entry.setStatus(SyncLog.Status.SKIPPED);
            entry.setMessage("Provider '" + provider.id() + "' has nothing to sync");
        } else if (!running.tryLock()) {
            entry.setStatus(SyncLog.Status.SKIPPED);
            entry.setMessage("A sync is already running");
        } else {
            try {
                List<ExternalFixture> fixtures = withRetry("fetchFixtures", provider::fetchFixtures);
                int upserted = upsertFixtures(fixtures);

                List<ExternalStandingsRow> standings = withRetry("fetchStandings", provider::fetchStandings);
                replaceStandings(standings);

                entry.setStatus(SyncLog.Status.SUCCESS);
                entry.setRecordsProcessed(fixtures.size());
                entry.setRecordsUpserted(upserted);
            } catch (Exception ex) {
                log.error("League sync failed (trigger={})", trigger, ex);
                entry.setStatus(SyncLog.Status.FAILED);
                entry.setMessage(ex.getMessage());
            } finally {
                running.unlock();
            }
        }

        entry.setFinishedAt(Instant.now());
        return syncLogRepository.save(entry);
    }

    public List<SyncLog> recentLogs() {
        return syncLogRepository.findTop50ByOrderByStartedAtDesc();
    }

    private int upsertFixtures(List<ExternalFixture> fixtures) {
        int upserted = 0;
        for (ExternalFixture external : fixtures) {
            if (Text.isBlank(external.externalId())) {
                continue;
            }
            Fixture existing = fixtureRepository.findByExternalId(external.externalId()).orElse(null);
            fixtureRepository.save(apply(external, existing));
            upserted++;
        }
        return upserted;
    }

    /** Package-private for tests: maps one league fixture onto a new or existing document. */
    static Fixture apply(ExternalFixture external, Fixture existing) {
        Fixture fixture = existing != null ? existing : new Fixture();
        Instant now = Instant.now();

        fixture.setExternalId(external.externalId());
        fixture.setSource(Fixture.Source.SYNC);
        fixture.setCompetition(external.competition());
        fixture.setSeason(external.season());
        fixture.setKickoffAt(external.kickoffAt());
        fixture.setVenue(external.venue());
        fixture.setStatus(external.status());

        // No team names means the source row was unusable: keep what we had.
        if (external.homeTeam() != null && external.awayTeam() != null) {
            fixture.setHomeTeam(external.homeTeam());
            fixture.setAwayTeam(external.awayTeam());
            fixture.setHomeScore(external.homeScore());
            fixture.setAwayScore(external.awayScore());
        }

        fixture.setLastSyncedAt(now);
        if (fixture.getCreatedAt() == null) {
            fixture.setCreatedAt(now);
        }
        fixture.setUpdatedAt(now);
        return fixture;
    }

    /**
     * Inserts the new table before deleting the old rows, so a failure part-way
     * leaves the previous table in place rather than an empty one.
     */
    private void replaceStandings(List<ExternalStandingsRow> rows) {
        if (rows.isEmpty()) {
            log.warn("Standings fetch returned no rows; leaving the existing table untouched");
            return;
        }
        List<String> oldIds = standingsRepository.findAll().stream().map(StandingsRow::getId).toList();
        standingsRepository.saveAll(rows.stream()
                .map(r -> new StandingsRow(null, r.position(), r.team(), r.played(), r.won(), r.drawn(), r.lost(),
                        r.scoreFor(), r.scoreAgainst(), r.scoreDifference(), r.points()))
                .toList());
        standingsRepository.deleteAllById(oldIds);
    }

    private <T> T withRetry(String operation, Supplier<T> action) {
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

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Sync retry interrupted", ex);
        }
    }

    /** Re-reads the interval for every scheduling decision, so admin changes apply from the next run. */
    private final class DynamicIntervalTrigger implements Trigger {
        @Override
        public Instant nextExecution(TriggerContext context) {
            Instant lastCompletion = context.lastCompletion();
            if (lastCompletion == null) {
                return Instant.now().plus(settings.initialDelay());
            }
            long interval;
            try {
                interval = getIntervalMs();
            } catch (RuntimeException ex) {
                // MongoDB briefly unreachable: fall back rather than stop scheduling for good.
                log.warn("Could not read the sync interval, using the default: {}", ex.getMessage());
                interval = settings.defaultInterval().toMillis();
            }
            return lastCompletion.plusMillis(interval);
        }
    }
}

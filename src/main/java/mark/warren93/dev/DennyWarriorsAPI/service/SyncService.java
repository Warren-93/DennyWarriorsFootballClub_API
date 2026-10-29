package mark.warren93.dev.DennyWarriorsAPI.service;

import jakarta.annotation.PostConstruct;
import mark.warren93.dev.DennyWarriorsAPI.config.ClubConfig;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    // A fixtures report whose newest match is older than this is almost
    // certainly still pointing at a finished season.
    private static final Duration STALE_FIXTURES_AFTER = Duration.ofDays(60);
    private static final String COMPETITION_ACTIVE = "ACTIVE";

    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_MANUAL = "MANUAL";

    private final LeagueApiClient leagueApiClient;
    private final LeagueDataMapper mapper;
    private final FixtureRepository fixtureRepository;
    private final StandingsRowRepository standingsRowRepository;
    private final SyncLogRepository syncLogRepository;
    private final SyncSettingsRepository syncSettingsRepository;
    private final TaskScheduler taskScheduler;
    private final ClubConfig clubConfig;

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
            TaskScheduler taskScheduler,
            ClubConfig clubConfig) {
        this.leagueApiClient = leagueApiClient;
        this.mapper = mapper;
        this.fixtureRepository = fixtureRepository;
        this.standingsRowRepository = standingsRowRepository;
        this.syncLogRepository = syncLogRepository;
        this.syncSettingsRepository = syncSettingsRepository;
        this.taskScheduler = taskScheduler;
        this.clubConfig = clubConfig;
    }

    @PostConstruct
    void init() {
        SyncSettings settings = syncSettingsRepository.findById(SyncSettings.SINGLETON_ID).orElse(null);
        // The settings document can exist without an interval (e.g. created by
        // saving Comet links first), so treat 0 as "not set" too.
        currentIntervalMs.set(settings != null && settings.getIntervalMs() > 0
                ? settings.getIntervalMs()
                : defaultIntervalMs);
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

        SyncSettings settings = syncSettingsRepository.loadOrCreate();
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
            checkFixturesAreCurrent(matches, syncLog.getWarnings());

            List<StandingsRow> standings = withRetry("fetchStandings", leagueApiClient::fetchStandings);
            replaceStandings(selectClubTable(standings, syncLog.getWarnings()));

            syncLog.setStatus("SUCCESS");
            syncLog.setRecordsProcessed(matches.size());
            syncLog.setRecordsUpserted(upserted);
        } catch (Exception ex) {
            log.error("League sync failed (trigger={})", trigger, ex);
            syncLog.setStatus("FAILED");
            syncLog.setErrorMessage(redactApiKey(ex.getMessage()));
        }
        syncLog.getWarnings().forEach(warning -> log.warn("League sync warning: {}", warning));

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

    /**
     * Each new season is a new Comet competition, and a fixtures report saved
     * against last season's competition keeps "working" — it just stops
     * returning anything new. Flag that so someone fetches a new link.
     */
    private void checkFixturesAreCurrent(List<MatchResult> matches, List<String> warnings) {
        if (matches.isEmpty()) {
            warnings.add("The Comet fixtures report returned no matches. If a new season has started, "
                    + "paste the new fixtures link under Comet report links.");
            return;
        }
        Long newest = matches.stream()
                .map(MatchResult::getMatchDate)
                .filter(Objects::nonNull)
                .max(Long::compare)
                .orElse(null);
        if (newest != null && Instant.ofEpochMilli(newest).isBefore(Instant.now().minus(STALE_FIXTURES_AFTER))) {
            LocalDate newestDate = Instant.ofEpochMilli(newest).atZone(ZoneOffset.UTC).toLocalDate();
            warnings.add("The newest match in the Comet fixtures report is from " + newestDate
                    + ", so it is probably still last season's report. Paste the new fixtures link under "
                    + "Comet report links.");
        }
    }

    /**
     * Picks the one league table to show. A report filtered on competition
     * type + status (rather than one fixed competition) rolls over by itself
     * each season, but can return several divisions' tables at once — keep
     * the one the club is in, preferring a competition that is still ACTIVE.
     */
    List<StandingsRow> selectClubTable(List<StandingsRow> rows, List<String> warnings) {
        if (rows.isEmpty()) {
            return rows;
        }
        Map<Long, List<StandingsRow>> byCompetition = new LinkedHashMap<>();
        for (StandingsRow row : rows) {
            byCompetition.computeIfAbsent(Objects.requireNonNullElse(row.getCompetitionId(), 0L), id -> new ArrayList<>())
                    .add(row);
        }

        List<List<StandingsRow>> withClub = byCompetition.values().stream()
                .filter(table -> table.stream().anyMatch(row -> clubConfig.isClub(row.getClubId(), row.getClub())))
                .toList();
        List<List<StandingsRow>> candidates = withClub.isEmpty() ? List.copyOf(byCompetition.values()) : withClub;
        List<StandingsRow> chosen = candidates.stream()
                .filter(SyncService::isActive)
                .findFirst()
                .orElse(candidates.get(0));

        String competition = describeCompetition(chosen.get(0));
        if (withClub.isEmpty()) {
            warnings.add("The club wasn't found in the " + competition + " table. Check DWFC_CLUB_COMET_ID and "
                    + "DWFC_CLUB_NAME match how Comet lists the club.");
        }
        if (byCompetition.size() > 1) {
            warnings.add("The Comet standings report returned " + byCompetition.size()
                    + " competitions; showing " + competition + ".");
        }
        if (chosen.get(0).getStatus() != null && !isActive(chosen)) {
            warnings.add(competition + " has finished in Comet, so the league table is that season's final table. "
                    + "Once the new season's league is set up, paste its standings link under Comet report links.");
        }
        return chosen;
    }

    private static boolean isActive(List<StandingsRow> table) {
        return table.stream().anyMatch(row -> COMPETITION_ACTIVE.equalsIgnoreCase(row.getStatus()));
    }

    private static String describeCompetition(StandingsRow row) {
        if (row.getCompetition() == null) {
            return "the league";
        }
        return row.getSeason() == null ? row.getCompetition() : row.getCompetition() + " (" + row.getSeason() + ")";
    }

    /** WebClient errors can include the request URL, and with it the Comet API key. */
    static String redactApiKey(String message) {
        return message == null ? null : message.replaceAll("API_KEY=[^&\\s]+", "API_KEY=<redacted>");
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

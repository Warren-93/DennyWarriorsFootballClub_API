package com.clubsite.api.sync;

import com.clubsite.api.TestProperties;
import com.clubsite.api.common.ApiException;
import com.clubsite.api.matches.Fixture;
import com.clubsite.api.matches.FixtureRepository;
import com.clubsite.api.matches.StandingsRowRepository;
import com.clubsite.api.sync.LeagueDataProvider.ExternalFixture;
import com.clubsite.api.sync.LeagueDataProvider.ExternalStandingsRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private LeagueDataProvider provider;
    @Mock
    private FixtureRepository fixtureRepository;
    @Mock
    private StandingsRowRepository standingsRepository;
    @Mock
    private SyncLogRepository syncLogRepository;
    @Mock
    private SyncSettingsRepository syncSettingsRepository;
    @Mock
    private TaskScheduler taskScheduler;

    private SyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new SyncService(provider, fixtureRepository, standingsRepository, syncLogRepository,
                syncSettingsRepository, taskScheduler, TestProperties.defaults());
        lenient().when(syncLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(provider.isRemote()).thenReturn(true);
        lenient().when(provider.id()).thenReturn("fake");
    }

    private ExternalFixture played(String id) {
        return new ExternalFixture(id, "Premier League", "2025/26", "Example FC", "Rivals United",
                Instant.parse("2026-08-20T14:00:00Z"), "Home Park", "PLAYED", 3, 0);
    }

    private ExternalStandingsRow row(int position) {
        return new ExternalStandingsRow(position, "Example FC", 1, 1, 0, 0, 3, 0, 3, 3);
    }

    @Test
    void successfulSyncUpsertsFixturesReplacesStandingsAndLogsSuccess() {
        when(provider.fetchFixtures()).thenReturn(List.of(played("1"), played("2")));
        when(provider.fetchStandings()).thenReturn(List.of(row(1)));
        when(fixtureRepository.findByExternalId(any())).thenReturn(Optional.empty());

        SyncLog result = syncService.sync(SyncLog.Trigger.MANUAL);

        verify(fixtureRepository, times(2)).save(any(Fixture.class));
        verify(standingsRepository).saveAll(anyList());
        verify(standingsRepository).deleteAllById(anyList());
        assertThat(result.getStatus()).isEqualTo(SyncLog.Status.SUCCESS);
        assertThat(result.getRecordsProcessed()).isEqualTo(2);
        assertThat(result.getRecordsUpserted()).isEqualTo(2);
        assertThat(result.getProvider()).isEqualTo("fake");
    }

    @Test
    void existingFixtureIsUpdatedInPlaceNotDuplicated() {
        Fixture existing = new Fixture();
        existing.setId("mongo-id-1");
        existing.setExternalId("1");
        when(provider.fetchFixtures()).thenReturn(List.of(played("1")));
        when(provider.fetchStandings()).thenReturn(List.of());
        when(fixtureRepository.findByExternalId("1")).thenReturn(Optional.of(existing));

        syncService.sync(SyncLog.Trigger.SCHEDULED);

        ArgumentCaptor<Fixture> captor = ArgumentCaptor.forClass(Fixture.class);
        verify(fixtureRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("mongo-id-1");
        assertThat(captor.getValue().getSource()).isEqualTo(Fixture.Source.SYNC);
    }

    @Test
    void missingTeamNamesKeepWhatWasStored() {
        Fixture existing = new Fixture();
        existing.setHomeTeam("Example FC");
        existing.setAwayTeam("Rivals United");
        existing.setHomeScore(3);
        existing.setAwayScore(0);
        existing.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        Fixture updated = SyncService.apply(
                new ExternalFixture("1", "League", "2025/26", null, null, null, null, "PLAYED", null, null), existing);

        assertThat(updated.getHomeTeam()).isEqualTo("Example FC");
        assertThat(updated.getHomeScore()).isEqualTo(3);
        assertThat(updated.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(updated.getLastSyncedAt()).isNotNull();
    }

    @Test
    void emptyStandingsResponseDoesNotWipeExistingData() {
        when(provider.fetchFixtures()).thenReturn(List.of());
        when(provider.fetchStandings()).thenReturn(List.of());

        SyncLog result = syncService.sync(SyncLog.Trigger.MANUAL);

        verify(standingsRepository, never()).deleteAllById(any());
        verify(standingsRepository, never()).saveAll(any());
        assertThat(result.getStatus()).isEqualTo(SyncLog.Status.SUCCESS);
    }

    @Test
    void persistentFailureLogsFailedAndTouchesNoData() {
        when(provider.fetchFixtures()).thenThrow(new RuntimeException("League API error: 503"));

        SyncLog result = syncService.sync(SyncLog.Trigger.SCHEDULED);

        assertThat(result.getStatus()).isEqualTo(SyncLog.Status.FAILED);
        assertThat(result.getMessage()).contains("503");
        verify(provider, times(3)).fetchFixtures();
        verify(fixtureRepository, never()).save(any());
        verifyNoInteractions(standingsRepository);
    }

    @Test
    void transientFailureThenSuccessStillSucceedsViaRetry() {
        when(provider.fetchFixtures())
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(List.of(played("1")));
        when(provider.fetchStandings()).thenReturn(List.of());
        when(fixtureRepository.findByExternalId(any())).thenReturn(Optional.empty());

        SyncLog result = syncService.sync(SyncLog.Trigger.SCHEDULED);

        assertThat(result.getStatus()).isEqualTo(SyncLog.Status.SUCCESS);
        verify(provider, times(2)).fetchFixtures();
    }

    @Test
    void manualProviderIsSkippedWithoutFetching() {
        when(provider.isRemote()).thenReturn(false);

        SyncLog result = syncService.sync(SyncLog.Trigger.MANUAL);

        assertThat(result.getStatus()).isEqualTo(SyncLog.Status.SKIPPED);
        verify(provider, never()).fetchFixtures();
    }

    @Test
    void manualProviderIsNeverScheduled() {
        when(provider.isRemote()).thenReturn(false);

        syncService.scheduleSync();

        verifyNoInteractions(taskScheduler);
    }

    @Test
    void intervalBelowTheMinimumIsRejected() {
        assertThatThrownBy(() -> syncService.updateInterval(Duration.ofHours(1)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("at least 1 day");
    }
}

package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import mark.warren93.dev.DennyWarriorsAPI.model.MatchResult;
import mark.warren93.dev.DennyWarriorsAPI.model.StandingsRow;
import mark.warren93.dev.DennyWarriorsAPI.model.SyncLog;
import mark.warren93.dev.DennyWarriorsAPI.repository.FixtureRepository;
import mark.warren93.dev.DennyWarriorsAPI.repository.StandingsRowRepository;
import mark.warren93.dev.DennyWarriorsAPI.repository.SyncLogRepository;
import mark.warren93.dev.DennyWarriorsAPI.repository.SyncSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private LeagueApiClient leagueApiClient;
    @Mock
    private FixtureRepository fixtureRepository;
    @Mock
    private StandingsRowRepository standingsRowRepository;
    @Mock
    private SyncLogRepository syncLogRepository;
    @Mock
    private SyncSettingsRepository syncSettingsRepository;
    @Mock
    private TaskScheduler taskScheduler;

    private SyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new SyncService(
                leagueApiClient, new LeagueDataMapper(), fixtureRepository, standingsRowRepository,
                syncLogRepository, syncSettingsRepository, taskScheduler);
        when(syncLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private MatchResult playedMatch(long matchId) {
        MatchResult match = new MatchResult();
        match.setMatchId(matchId);
        match.setMatchDescription("Denny Warriors - Edinburgh Thistle 3:0");
        match.setMatchStatus("PLAYED");
        match.setSeason("2025/2026");
        match.setName("Appin Warriors Premier League 25/26");
        return match;
    }

    private StandingsRow row(int position) {
        StandingsRow row = new StandingsRow();
        row.setPosition(position);
        row.setClub("Denny Warriors");
        return row;
    }

    @Test
    void successfulSyncUpsertsFixturesAndReplacesStandingsAndLogsSuccess() {
        when(leagueApiClient.fetchMatches()).thenReturn(List.of(playedMatch(1), playedMatch(2)));
        when(leagueApiClient.fetchStandings()).thenReturn(List.of(row(1)));
        when(fixtureRepository.findByLeagueFixtureId(any())).thenReturn(Optional.empty());

        SyncLog result = syncService.sync(SyncService.TRIGGER_MANUAL);

        verify(fixtureRepository, times(2)).save(any(Fixture.class));
        verify(standingsRowRepository).deleteAll();
        verify(standingsRowRepository).saveAll(List.of(row(1)));
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getRecordsProcessed()).isEqualTo(2);
        assertThat(result.getRecordsUpserted()).isEqualTo(2);
        assertThat(result.getTrigger()).isEqualTo(SyncService.TRIGGER_MANUAL);
    }

    @Test
    void existingFixtureIsUpdatedInPlaceNotDuplicated() {
        Fixture existing = new Fixture();
        existing.setId("mongo-id-1");
        existing.setLeagueFixtureId("1");

        when(leagueApiClient.fetchMatches()).thenReturn(List.of(playedMatch(1)));
        when(leagueApiClient.fetchStandings()).thenReturn(List.of());
        when(fixtureRepository.findByLeagueFixtureId("1")).thenReturn(Optional.of(existing));

        syncService.sync(SyncService.TRIGGER_SCHEDULED);

        ArgumentCaptor<Fixture> captor = ArgumentCaptor.forClass(Fixture.class);
        verify(fixtureRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("mongo-id-1");
    }

    @Test
    void emptyStandingsResponseDoesNotWipeExistingData() {
        when(leagueApiClient.fetchMatches()).thenReturn(List.of());
        when(leagueApiClient.fetchStandings()).thenReturn(List.of());

        SyncLog result = syncService.sync(SyncService.TRIGGER_MANUAL);

        verify(standingsRowRepository, never()).deleteAll();
        verify(standingsRowRepository, never()).saveAll(any());
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void persistentFetchFailureLogsFailedAndTouchesNoData() {
        when(leagueApiClient.fetchMatches()).thenThrow(new RuntimeException("Comet API error: 503"));

        SyncLog result = syncService.sync(SyncService.TRIGGER_SCHEDULED);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).contains("503");
        verify(fixtureRepository, never()).save(any());
        verify(standingsRowRepository, never()).deleteAll();
    }

    @Test
    void transientFailureThenSuccessStillSucceedsViaRetry() {
        when(leagueApiClient.fetchMatches())
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(List.of(playedMatch(1)));
        when(leagueApiClient.fetchStandings()).thenReturn(List.of());
        when(fixtureRepository.findByLeagueFixtureId(any())).thenReturn(Optional.empty());

        SyncLog result = syncService.sync(SyncService.TRIGGER_SCHEDULED);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(leagueApiClient, times(2)).fetchMatches();
        verify(fixtureRepository, times(1)).save(any());
    }
}

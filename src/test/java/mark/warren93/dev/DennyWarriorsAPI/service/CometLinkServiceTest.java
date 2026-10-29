package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.config.CometApiConfig;
import mark.warren93.dev.DennyWarriorsAPI.dto.CometLinksResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.UpdateCometLinksRequest;
import mark.warren93.dev.DennyWarriorsAPI.exception.InvalidSyncSettingsException;
import mark.warren93.dev.DennyWarriorsAPI.model.SyncSettings;
import mark.warren93.dev.DennyWarriorsAPI.repository.SyncSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CometLinkServiceTest {

    private static final String BASE = "https://www.scottishfacomet.co.uk/data-backend/api/public/areports/run/";
    private static final String ENV_LINK = BASE + "0/25/?API_KEY=envkey00001111";
    private static final String ADMIN_LINK = BASE + "0/25/?API_KEY=adminkey9999&Status=ACTIVE";

    @Mock
    private SyncSettingsRepository syncSettingsRepository;

    private CometApiConfig envConfig;
    private CometLinkService service;

    @BeforeEach
    void setUp() {
        envConfig = new CometApiConfig();
        envConfig.setFixturesUrl(ENV_LINK);
        envConfig.setStandingsUrl("");
        service = new CometLinkService(envConfig, syncSettingsRepository);
        lenient().when(syncSettingsRepository.loadOrCreate()).thenCallRealMethod();
    }

    private void storedSettings(String fixturesUrl, String standingsUrl) {
        SyncSettings settings = new SyncSettings();
        settings.setId(SyncSettings.SINGLETON_ID);
        settings.setCometFixturesUrl(fixturesUrl);
        settings.setCometStandingsUrl(standingsUrl);
        when(syncSettingsRepository.findById(SyncSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));
    }

    @Test
    void adminPanelLinkWinsOverEnvironmentAndAsksForTheMaxPageSize() {
        storedSettings(ADMIN_LINK, null);

        assertThat(service.resolve(CometLinkService.Report.FIXTURES))
                .isEqualTo(BASE + "0/1000/?API_KEY=adminkey9999&Status=ACTIVE");
    }

    @Test
    void fallsBackToEnvironmentThenFailsWithAHelpfulMessage() {
        storedSettings(null, null);

        assertThat(service.resolve(CometLinkService.Report.FIXTURES)).isEqualTo(BASE + "0/1000/?API_KEY=envkey00001111");
        assertThatThrownBy(() -> service.resolve(CometLinkService.Report.STANDINGS))
                .hasMessageContaining("COMET_STANDINGS_URL is not set");
    }

    @Test
    void describeNeverReturnsTheKey() {
        storedSettings(ADMIN_LINK, null);

        CometLinksResponse links = service.describe();

        assertThat(links.fixtures()).isEqualTo(new CometLinksResponse.LinkStatus("ADMIN_PANEL", "…9999", "Status=ACTIVE"));
        assertThat(links.standings().source()).isEqualTo("NOT_SET");
        assertThat(links.toString()).doesNotContain("adminkey");
    }

    @Test
    void updateStoresHttpsLinkAndBlankClearsBackToEnvironment() {
        storedSettings(null, ADMIN_LINK);

        service.update(new UpdateCometLinksRequest("http://www.scottishfacomet.co.uk/data-backend/api/public/areports/run/0/25/?API_KEY=k", ""));

        ArgumentCaptor<SyncSettings> saved = ArgumentCaptor.forClass(SyncSettings.class);
        verify(syncSettingsRepository).save(saved.capture());
        assertThat(saved.getValue().getCometFixturesUrl()).startsWith("https://www.scottishfacomet.co.uk/");
        assertThat(saved.getValue().getCometStandingsUrl()).isNull();
    }

    @Test
    void rejectsLinksThatAreNotCometReports() {
        assertThatThrownBy(() -> CometLinkService.validate("https://evil.example.com/data-backend/api/public/areports/run/0/25/?API_KEY=k"))
                .isInstanceOf(InvalidSyncSettingsException.class);
        assertThatThrownBy(() -> CometLinkService.validate("https://scottishfacomet.co.uk.evil.com/data-backend/api/public/areports/run/0/25/?API_KEY=k"))
                .isInstanceOf(InvalidSyncSettingsException.class);
        assertThatThrownBy(() -> CometLinkService.validate(BASE + "0/25/"))
                .hasMessageContaining("no API_KEY");
    }

    @Test
    void savingLinksFirstDoesNotTouchTheInterval() {
        when(syncSettingsRepository.findById(SyncSettings.SINGLETON_ID)).thenReturn(Optional.empty());
        when(syncSettingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(new UpdateCometLinksRequest(ADMIN_LINK, null));

        ArgumentCaptor<SyncSettings> saved = ArgumentCaptor.forClass(SyncSettings.class);
        verify(syncSettingsRepository).save(saved.capture());
        assertThat(saved.getValue().getIntervalMs()).isZero();
        assertThat(saved.getValue().getCometStandingsUrl()).isNull();
    }
}

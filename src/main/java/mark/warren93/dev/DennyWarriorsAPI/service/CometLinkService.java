package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.config.CometApiConfig;
import mark.warren93.dev.DennyWarriorsAPI.dto.CometLinksResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.UpdateCometLinksRequest;
import mark.warren93.dev.DennyWarriorsAPI.exception.InvalidSyncSettingsException;
import mark.warren93.dev.DennyWarriorsAPI.model.SyncSettings;
import mark.warren93.dev.DennyWarriorsAPI.repository.SyncSettingsRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Decides which Comet report link each sync uses. A link pasted in the admin
 * panel (stored in SyncSettings) wins over the COMET_*_URL environment
 * variable, so rolling over to a new season's report is a paste, not a
 * Render redeploy.
 *
 * Each link is a saved Comet "My Reports" report whose API_KEY carries that
 * report's filters, which is why a new season normally needs a new link.
 */
@Service
public class CometLinkService {

    public enum Report {
        FIXTURES("COMET_FIXTURES_URL", SyncSettings::getCometFixturesUrl, SyncSettings::setCometFixturesUrl),
        STANDINGS("COMET_STANDINGS_URL", SyncSettings::getCometStandingsUrl, SyncSettings::setCometStandingsUrl);

        private final String envVar;
        private final Function<SyncSettings, String> getter;
        private final BiConsumer<SyncSettings, String> setter;

        Report(String envVar, Function<SyncSettings, String> getter, BiConsumer<SyncSettings, String> setter) {
            this.envVar = envVar;
            this.getter = getter;
            this.setter = setter;
        }
    }

    public static final String SOURCE_ADMIN = "ADMIN_PANEL";
    public static final String SOURCE_ENV = "ENVIRONMENT";
    public static final String SOURCE_NONE = "NOT_SET";

    // Comet's documented maximum page size. The links Comet hands out ask for
    // 25 rows (".../run/0/25/"), which silently drops fixtures past the 25th.
    static final int MAX_PAGE_SIZE = 1000;
    private static final Pattern PAGE_SEGMENT = Pattern.compile("/areports/run/\\d+/\\d+/?");
    private static final String REPORT_PATH = "/data-backend/api/public/areports/run/";
    // Only ever fetch from the Scottish FA's Comet, never an arbitrary host an
    // admin (or a stolen admin token) pastes in.
    private static final String ALLOWED_HOST = "scottishfacomet.co.uk";

    private final CometApiConfig envConfig;
    private final SyncSettingsRepository syncSettingsRepository;

    public CometLinkService(CometApiConfig envConfig, SyncSettingsRepository syncSettingsRepository) {
        this.envConfig = envConfig;
        this.syncSettingsRepository = syncSettingsRepository;
    }

    /**
     * The link to fetch for a report, with the page size raised to the max.
     * Throws when neither source has one, which fails the sync (not the whole
     * API) with a message that shows up in the admin panel's sync log.
     */
    public String resolve(Report report) {
        String url = adminValue(report);
        if (url == null) {
            url = envValue(report);
        }
        if (url == null) {
            throw new IllegalStateException(report.envVar
                    + " is not set — paste the report link under Comet report links in the admin panel");
        }
        return withMaxPageSize(url);
    }

    public CometLinksResponse describe() {
        return new CometLinksResponse(status(Report.FIXTURES), status(Report.STANDINGS));
    }

    public CometLinksResponse update(UpdateCometLinksRequest request) {
        SyncSettings settings = syncSettingsRepository.loadOrCreate();
        apply(settings, Report.FIXTURES, request.fixturesUrl());
        apply(settings, Report.STANDINGS, request.standingsUrl());
        settings.setUpdatedAt(Instant.now());
        syncSettingsRepository.save(settings);
        return describe();
    }

    private void apply(SyncSettings settings, Report report, String requested) {
        if (requested == null) {
            return;
        }
        report.setter.accept(settings, requested.isBlank() ? null : validate(requested));
    }

    private CometLinksResponse.LinkStatus status(Report report) {
        String admin = adminValue(report);
        if (admin != null) {
            return new CometLinksResponse.LinkStatus(SOURCE_ADMIN, keyHint(admin), filters(admin));
        }
        String env = envValue(report);
        if (env != null) {
            return new CometLinksResponse.LinkStatus(SOURCE_ENV, keyHint(env), filters(env));
        }
        return new CometLinksResponse.LinkStatus(SOURCE_NONE, null, null);
    }

    private String adminValue(Report report) {
        return syncSettingsRepository.findById(SyncSettings.SINGLETON_ID)
                .map(report.getter)
                .filter(url -> !url.isBlank())
                .orElse(null);
    }

    private String envValue(Report report) {
        String url = report == Report.FIXTURES ? envConfig.getFixturesUrl() : envConfig.getStandingsUrl();
        // "${" means the placeholder wasn't resolved at all.
        return url == null || url.isBlank() || url.contains("${") ? null : url;
    }

    /** Checks a pasted link is a Comet report link and normalises it to https. */
    static String validate(String rawUrl) {
        String url = rawUrl.trim();
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            throw new InvalidSyncSettingsException("That doesn't look like a link: " + ex.getReason());
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean hostAllowed = host.equals(ALLOWED_HOST) || host.endsWith("." + ALLOWED_HOST);
        boolean schemeAllowed = "https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme());
        if (!schemeAllowed || !hostAllowed || uri.getPath() == null || !uri.getPath().startsWith(REPORT_PATH)) {
            throw new InvalidSyncSettingsException(
                    "Comet links look like https://www.scottishfacomet.co.uk" + REPORT_PATH + "0/25/?API_KEY=...");
        }
        if (uri.getRawQuery() == null || !uri.getRawQuery().contains("API_KEY=")) {
            throw new InvalidSyncSettingsException("That Comet link has no API_KEY — use Copy API Code on the report in Comet");
        }
        // Comet's "Copy API Code" sometimes hands out http:// — the key is in
        // the query string, so never send it in the clear.
        return "http".equalsIgnoreCase(uri.getScheme()) ? "https" + url.substring(4) : url;
    }

    static String withMaxPageSize(String url) {
        return PAGE_SEGMENT.matcher(url).replaceFirst("/areports/run/0/" + MAX_PAGE_SIZE + "/");
    }

    /** Last four characters of the API key, enough to tell two links apart. */
    static String keyHint(String url) {
        String key = queryParam(url, "API_KEY");
        return key == null || key.length() < 8 ? null : "…" + key.substring(key.length() - 4);
    }

    /** The link's query string without the API key, e.g. "Status=ACTIVE". */
    static String filters(String url) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return "";
        }
        return Arrays.stream(url.substring(queryStart + 1).split("&"))
                .filter(part -> !part.isBlank() && !part.startsWith("API_KEY="))
                .collect(Collectors.joining("&"));
    }

    private static String queryParam(String url, String name) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return null;
        }
        return Arrays.stream(url.substring(queryStart + 1).split("&"))
                .filter(part -> part.startsWith(name + "="))
                .map(part -> part.substring(name.length() + 1))
                .findFirst()
                .orElse(null);
    }
}

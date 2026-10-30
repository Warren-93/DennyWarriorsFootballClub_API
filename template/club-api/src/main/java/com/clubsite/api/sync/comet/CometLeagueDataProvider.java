package com.clubsite.api.sync.comet;

import com.clubsite.api.common.Text;
import com.clubsite.api.config.ClubProperties;
import com.clubsite.api.sync.LeagueDataProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Comet (used by the Scottish FA and several other associations) public
 * report API. Select with LEAGUE_PROVIDER=comet and set COMET_FIXTURES_URL
 * and COMET_STANDINGS_URL. Each URL carries an API key, so they are only
 * ever environment variables.
 */
@Component
@ConditionalOnProperty(name = "club.league.provider", havingValue = "comet")
public class CometLeagueDataProvider implements LeagueDataProvider {

    private final RestClient restClient;
    private final ClubProperties.Comet config;

    public CometLeagueDataProvider(RestClient.Builder restClientBuilder, ClubProperties properties) {
        this.restClient = restClientBuilder.build();
        this.config = properties.league().comet();
    }

    @Override
    public String id() {
        return "comet";
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public List<ExternalFixture> fetchFixtures() {
        var response = restClient.get()
                .uri(requireUrl(config.fixturesUrl(), "COMET_FIXTURES_URL"))
                .retrieve()
                .body(CometPayloads.MatchesResponse.class);
        if (response == null || response.results() == null) {
            return List.of();
        }
        return response.results().stream().filter(Objects::nonNull).map(CometLeagueDataProvider::toExternal).toList();
    }

    @Override
    public List<ExternalStandingsRow> fetchStandings() {
        var response = restClient.get()
                .uri(requireUrl(config.standingsUrl(), "COMET_STANDINGS_URL"))
                .retrieve()
                .body(CometPayloads.StandingsResponse.class);
        if (response == null || response.results() == null) {
            return List.of();
        }
        return response.results().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CometPayloads.StandingsRow::position, Comparator.nullsLast(Integer::compareTo)))
                .map(r -> new ExternalStandingsRow(r.position(), r.club(), r.matches(), r.wins(), r.draws(),
                        r.losses(), r.goalsFor(), r.goalsAgainst(), r.goalDifference(), r.points()))
                .toList();
    }

    static ExternalFixture toExternal(CometPayloads.Match match) {
        CometDescriptionParser.Parsed parsed = CometDescriptionParser.parse(match.matchDescription());
        return new ExternalFixture(
                match.matchId() == null ? null : String.valueOf(match.matchId()),
                match.name(),
                match.season(),
                parsed.homeTeam(),
                parsed.awayTeam(),
                match.matchDate() == null ? null : Instant.ofEpochMilli(match.matchDate()),
                match.facility(),
                match.matchStatus(),
                parsed.homeScore(),
                parsed.awayScore());
    }

    /**
     * Fails the sync (not the whole app) with a clear message when a URL is
     * missing, so the problem shows up in the admin panel's sync log.
     */
    private static URI requireUrl(String url, String envVar) {
        if (Text.isBlank(url)) {
            throw new IllegalStateException(envVar + " is not set");
        }
        return URI.create(url);
    }
}

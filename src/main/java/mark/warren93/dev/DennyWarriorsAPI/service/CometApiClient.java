package mark.warren93.dev.DennyWarriorsAPI.service;

import lombok.RequiredArgsConstructor;
import mark.warren93.dev.DennyWarriorsAPI.model.MatchResult;
import mark.warren93.dev.DennyWarriorsAPI.model.StandingsRow;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

/**
 * The real {@link LeagueApiClient}, backed by the Comet (Scottish FA) public
 * report API. Only {@link SyncService} calls this now — public controllers
 * read from MongoDB, never Comet directly.
 */
@Service
@RequiredArgsConstructor
public class CometApiClient implements LeagueApiClient {

    private final WebClient webClient;
    private final CometLinkService cometLinkService;

    private static Mono<? extends Throwable> cometError(String body) {
        return Mono.error(new RuntimeException("Comet API error: " + body));
    }

    @Override
    public List<MatchResult> fetchMatches() {
        MatchesResponse response = webClient.get()
                .uri(cometLinkService.resolve(CometLinkService.Report.FIXTURES))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).flatMap(CometApiClient::cometError))
                .bodyToMono(MatchesResponse.class)
                .block();
        return response != null && response.getResults() != null ? response.getResults() : List.of();
    }

    @Override
    public List<StandingsRow> fetchStandings() {
        StandingsResponse response = webClient.get()
                .uri(cometLinkService.resolve(CometLinkService.Report.STANDINGS))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).flatMap(CometApiClient::cometError))
                .bodyToMono(StandingsResponse.class)
                .block();
        return response != null && response.getResults() != null
                ? response.getResults().stream().sorted(Comparator.comparing(StandingsRow::getPosition)).toList()
                : List.of();
    }
}

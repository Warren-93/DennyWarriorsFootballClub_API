package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.model.MatchResult;
import mark.warren93.dev.DennyWarriorsAPI.model.StandingsRow;

import java.util.List;

/**
 * Abstraction over the league's data source, so the sync/mapping logic
 * doesn't depend on Comet specifically. {@link CometApiClient} is the one
 * real implementation; tests can supply a fake.
 */
public interface LeagueApiClient {

    List<MatchResult> fetchMatches();

    List<StandingsRow> fetchStandings();
}

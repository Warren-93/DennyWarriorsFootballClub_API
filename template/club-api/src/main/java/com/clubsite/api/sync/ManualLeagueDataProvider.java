package com.clubsite.api.sync;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The default: fixtures, results and the table are entered in the admin
 * panel, and there is nothing to sync.
 */
@Component
@ConditionalOnProperty(name = "club.league.provider", havingValue = "manual", matchIfMissing = true)
public class ManualLeagueDataProvider implements LeagueDataProvider {

    @Override
    public String id() {
        return "manual";
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public List<ExternalFixture> fetchFixtures() {
        return List.of();
    }

    @Override
    public List<ExternalStandingsRow> fetchStandings() {
        return List.of();
    }
}

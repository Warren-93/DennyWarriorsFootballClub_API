package com.clubsite.api.sync.comet;

import com.clubsite.api.sync.LeagueDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** LEAGUE_PROVIDER=comet swaps the provider without any code change. */
@SpringBootTest(properties = "club.league.provider=comet")
@ActiveProfiles("test")
class CometProviderSelectionTest {

    @Autowired
    private LeagueDataProvider provider;

    @Test
    void cometProviderIsTheOnlyProvider() {
        assertThat(provider).isInstanceOf(CometLeagueDataProvider.class);
        assertThat(provider.isRemote()).isTrue();
    }
}

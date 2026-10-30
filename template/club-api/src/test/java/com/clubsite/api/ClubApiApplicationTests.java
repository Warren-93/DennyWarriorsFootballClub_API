package com.clubsite.api;

import com.clubsite.api.config.ClubProperties;
import com.clubsite.api.sync.LeagueDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClubApiApplicationTests {

	@Autowired
	private ClubProperties properties;

	@Autowired
	private LeagueDataProvider provider;

	@Test
	void contextLoadsWithDefaults() {
		assertThat(properties.name()).isEqualTo("Example FC");
		assertThat(properties.timezone()).isEqualTo(ZoneId.of("Europe/London"));
		assertThat(properties.scoring().pointsForWin()).isEqualTo(3);
		assertThat(properties.security().jwt().accessTokenTtl()).isEqualTo(Duration.ofHours(1));
		assertThat(properties.media().maxFileSize().toMegabytes()).isEqualTo(5);
		assertThat(provider.id()).isEqualTo("manual");
	}
}

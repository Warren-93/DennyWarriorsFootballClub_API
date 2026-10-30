package mark.warren93.dev.DennyWarriorsAPI.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * The club's own name, used to derive club-relative fields (opponent, home/
 * away, goalsFor/goalsAgainst) from the neutral home/away Fixture data
 * synced from Comet.
 */
@Configuration
@ConfigurationProperties(prefix = "dwfc.club")
@Getter
@Setter
public class ClubConfig {
    private String name;
}

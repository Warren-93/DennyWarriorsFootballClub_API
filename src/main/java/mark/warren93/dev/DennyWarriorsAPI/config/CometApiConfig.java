package mark.warren93.dev.DennyWarriorsAPI.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "comet.api")
@Getter
@Setter
public class CometApiConfig {
    private String fixturesUrl;
    private String standingsUrl;
}

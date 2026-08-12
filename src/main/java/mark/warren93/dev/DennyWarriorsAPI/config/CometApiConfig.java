package mark.warren93.dev.DennyWarriorsAPI.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "comet.api")
@Data
public class CometApiConfig {
    private String fixturesUrl;
    private String standingsUrl;
}

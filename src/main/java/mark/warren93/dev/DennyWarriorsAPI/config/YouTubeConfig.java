package mark.warren93.dev.DennyWarriorsAPI.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * The club's YouTube channel, used to build both the frontend's iframe
 * embeds (Live/uploads playlist — those URLs are built client-side, see
 * the frontend's src/config/youtube.js) and the server-side video list
 * fetched here from the channel's public Atom feed.
 */
@Configuration
@ConfigurationProperties(prefix = "youtube")
@Getter
@Setter
public class YouTubeConfig {
    private String channelId;
}

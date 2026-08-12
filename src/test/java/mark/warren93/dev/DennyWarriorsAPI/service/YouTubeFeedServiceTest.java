package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.config.YouTubeConfig;
import mark.warren93.dev.DennyWarriorsAPI.dto.YouTubeVideo;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeFeedServiceTest {

    // parse() doesn't touch WebClient/config — only getVideos() does — so a
    // fully-constructed-but-unused WebClient/YouTubeConfig is fine here.
    private final YouTubeFeedService service = new YouTubeFeedService(null, new YouTubeConfig());

    @Test
    void parsesRealCapturedFeedIntoVideos() throws Exception {
        String xml = Files.readString(Path.of("src/test/resources/fixtures/youtube-feed-sample.xml"));

        List<YouTubeVideo> videos = service.parse(xml);

        assertThat(videos).hasSize(15);

        YouTubeVideo first = videos.get(0);
        assertThat(first.videoId()).isEqualTo("8fMLnDZF-Sg");
        assertThat(first.title()).isEqualTo("Denny Warriors vs Dunipace");
        assertThat(first.watchUrl()).isEqualTo("https://www.youtube.com/watch?v=8fMLnDZF-Sg");
        assertThat(first.thumbnailUrl()).isEqualTo("https://i.ytimg.com/vi/8fMLnDZF-Sg/hqdefault.jpg");
        assertThat(first.publishedAt()).isEqualTo("2026-08-10T01:22:52+00:00");
    }

    @Test
    void blankFeedProducesEmptyList() throws Exception {
        assertThat(service.parse("")).isEmpty();
        assertThat(service.parse(null)).isEmpty();
    }

    @Test
    void malformedXmlThrowsRatherThanSilentlyReturningNothing() {
        // getVideos() is the layer responsible for catching this and falling
        // back to the previous cached result — parse() itself should still
        // surface a real parse failure rather than swallow it.
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> service.parse("<not-valid-xml"));
    }
}

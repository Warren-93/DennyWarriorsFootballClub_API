package com.clubsite.api.media;

import com.clubsite.api.TestProperties;
import com.clubsite.api.media.YouTubeFeedService.YouTubeVideo;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeFeedServiceTest {

    // TestProperties has no channel id, so getVideos() never makes a request.
    private final YouTubeFeedService service = new YouTubeFeedService(RestClient.builder(), TestProperties.defaults());

    @Test
    void parsesACapturedFeed() throws Exception {
        String xml = Files.readString(Path.of("src/test/resources/fixtures/youtube-feed-sample.xml"));

        List<YouTubeVideo> videos = service.parse(xml);

        assertThat(videos).hasSize(15);
        YouTubeVideo first = videos.get(0);
        assertThat(first.videoId()).isEqualTo("8fMLnDZF-Sg");
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
    void noChannelConfiguredMeansNoVideos() {
        assertThat(service.getVideos()).isEmpty();
    }

    @Test
    void refusesDoctypeDeclarations() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE f [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><feed>&x;</feed>";

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.parse(xxe)).isInstanceOf(Exception.class);
    }
}

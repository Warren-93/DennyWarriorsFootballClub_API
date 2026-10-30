package com.clubsite.api.media;

import com.clubsite.api.common.Text;
import com.clubsite.api.config.ClubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists the club's recent YouTube uploads from the channel's public Atom
 * feed: no API key needed, unlike the YouTube Data API. Fetched server-side
 * because the feed sends no CORS headers, and cached so a YouTube hiccup
 * serves the previous list rather than an error.
 */
@Service
public class YouTubeFeedService {

    public record YouTubeVideo(String videoId, String title, String thumbnailUrl, String watchUrl, String publishedAt) {
    }

    private static final Logger log = LoggerFactory.getLogger(YouTubeFeedService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final String FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id={channelId}";

    private final RestClient restClient;
    private final String channelId;

    private volatile List<YouTubeVideo> cached = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public YouTubeFeedService(RestClient.Builder restClientBuilder, ClubProperties properties) {
        this.restClient = restClientBuilder.build();
        this.channelId = properties.youtube().channelId();
    }

    public List<YouTubeVideo> getVideos() {
        if (Text.isBlank(channelId)) {
            return List.of();
        }
        if (Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cached;
        }

        try {
            String xml = restClient.get().uri(FEED_URL, channelId).retrieve().body(String.class);
            cached = parse(xml);
        } catch (Exception ex) {
            log.warn("Failed to fetch/parse YouTube feed; serving previous result ({} videos)", cached.size(), ex);
        }
        // Set on failure too, so a YouTube outage is retried every 30 minutes
        // rather than on every page view.
        cachedAt = Instant.now();
        return cached;
    }

    // Package-private so the test can parse a captured feed without HTTP.
    List<YouTubeVideo> parse(String xml) throws Exception {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE hardening: this is external content, never resolve DTDs or entities.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        NodeList entries = document.getElementsByTagName("entry");
        List<YouTubeVideo> videos = new ArrayList<>();
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            String videoId = textOf(entry, "yt:videoId");
            if (Text.isBlank(videoId)) {
                continue;
            }
            videos.add(new YouTubeVideo(
                    videoId,
                    textOf(entry, "title"),
                    "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg",
                    "https://www.youtube.com/watch?v=" + videoId,
                    textOf(entry, "published")));
        }
        return videos;
    }

    private static String textOf(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }
}

package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.config.YouTubeConfig;
import mark.warren93.dev.DennyWarriorsAPI.dto.YouTubeVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists the club's recent YouTube uploads by fetching the channel's public
 * Atom feed (youtube.com/feeds/videos.xml) — no API key needed, unlike the
 * YouTube Data API. The feed only covers the ~15 most recent uploads, which
 * is fine for a "latest videos" grid; fetched server-side both because the
 * feed doesn't send CORS headers for direct browser fetches, and so a
 * transient YouTube hiccup doesn't take down the page (cached response is
 * served instead — see CACHE_TTL).
 */
@Service
public class YouTubeFeedService {

    private static final Logger log = LoggerFactory.getLogger(YouTubeFeedService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final String FEED_URL = "https://www.youtube.com/feeds/videos.xml?channel_id=";

    private final WebClient webClient;
    private final YouTubeConfig youTubeConfig;

    private volatile List<YouTubeVideo> cached = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public YouTubeFeedService(WebClient webClient, YouTubeConfig youTubeConfig) {
        this.webClient = webClient;
        this.youTubeConfig = youTubeConfig;
    }

    public List<YouTubeVideo> getVideos() {
        if (Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cached;
        }

        try {
            String xml = webClient.get()
                    .uri(FEED_URL + youTubeConfig.getChannelId())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            cached = parse(xml);
            cachedAt = Instant.now();
        } catch (Exception ex) {
            log.warn("Failed to fetch/parse YouTube feed; serving previous result ({} videos)", cached.size(), ex);
        }

        return cached;
    }

    // Package-private (not private) so YouTubeFeedServiceTest can exercise the
    // parsing logic directly against a captured sample feed, without needing
    // to mock WebClient's fluent chain.
    List<YouTubeVideo> parse(String xml) throws Exception {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE hardening — this is external content, don't resolve DTDs/external entities.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xml)));

        NodeList entries = document.getElementsByTagName("entry");
        List<YouTubeVideo> videos = new ArrayList<>();
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            String videoId = textOf(entry, "yt:videoId");
            if (videoId == null || videoId.isBlank()) {
                continue;
            }
            String title = textOf(entry, "title");
            String published = textOf(entry, "published");
            videos.add(new YouTubeVideo(
                    videoId,
                    title,
                    "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg",
                    "https://www.youtube.com/watch?v=" + videoId,
                    published));
        }
        return videos;
    }

    private String textOf(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }
}

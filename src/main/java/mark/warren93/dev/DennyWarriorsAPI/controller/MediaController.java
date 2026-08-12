package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.dto.YouTubeVideo;
import mark.warren93.dev.DennyWarriorsAPI.model.MediaAsset;
import mark.warren93.dev.DennyWarriorsAPI.service.MediaService;
import mark.warren93.dev.DennyWarriorsAPI.service.YouTubeFeedService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService mediaService;
    private final YouTubeFeedService youTubeFeedService;

    public MediaController(MediaService mediaService, YouTubeFeedService youTubeFeedService) {
        this.mediaService = mediaService;
        this.youTubeFeedService = youTubeFeedService;
    }

    @GetMapping("/videos")
    public List<YouTubeVideo> getVideos() {
        return youTubeFeedService.getVideos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getMedia(@PathVariable String id) {
        MediaAsset asset = mediaService.getById(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(asset.getData());
    }
}

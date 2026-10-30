package com.clubsite.api.media;

import com.clubsite.api.media.YouTubeFeedService.YouTubeVideo;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MediaController {

    public record MediaUploadResponse(String id, String filename, String contentType, long size, String url) {
    }

    private final MediaService mediaService;
    private final YouTubeFeedService youTubeFeedService;

    public MediaController(MediaService mediaService, YouTubeFeedService youTubeFeedService) {
        this.mediaService = mediaService;
        this.youTubeFeedService = youTubeFeedService;
    }

    @GetMapping("/media/videos")
    public List<YouTubeVideo> videos() {
        return youTubeFeedService.getVideos();
    }

    @GetMapping("/media/{id}")
    public ResponseEntity<byte[]> get(@PathVariable String id) {
        MediaAsset asset = mediaService.getById(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                // Assets are immutable (a new upload gets a new id), so cache hard.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(asset.getData());
    }

    /**
     * Returns an absolute URL, so the client can store it as-is in an
     * imageUrl field. Built from the incoming request, which respects
     * X-Forwarded-* headers behind a proxy (server.forward-headers-strategy).
     */
    @PostMapping("/admin/media")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaUploadResponse upload(@RequestParam("file") MultipartFile file) {
        MediaAsset asset = mediaService.upload(file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/media/{id}")
                .buildAndExpand(asset.getId())
                .toUriString();
        return new MediaUploadResponse(asset.getId(), asset.getFilename(), asset.getContentType(), asset.getSize(), url);
    }
}

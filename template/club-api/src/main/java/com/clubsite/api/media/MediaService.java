package com.clubsite.api.media;

import com.clubsite.api.common.ApiException;
import com.clubsite.api.config.ClubProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
public class MediaService {

    private final MediaAssetRepository repository;
    private final long maxFileSizeBytes;
    private final Set<String> allowedContentTypes;

    public MediaService(MediaAssetRepository repository, ClubProperties properties) {
        this.repository = repository;
        this.maxFileSizeBytes = properties.media().maxFileSize().toBytes();
        this.allowedContentTypes = properties.media().allowedContentTypes();
    }

    public MediaAsset upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No file provided");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw ApiException.badRequest("File exceeds the " + (maxFileSizeBytes / (1024 * 1024)) + "MB size limit");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!allowedContentTypes.contains(contentType)) {
            throw ApiException.badRequest("Unsupported file type: " + contentType + " (allowed: " + allowedContentTypes + ")");
        }

        MediaAsset asset = new MediaAsset();
        asset.setFilename(file.getOriginalFilename());
        asset.setContentType(contentType);
        asset.setSize(file.getSize());
        asset.setUploadedAt(Instant.now());
        try {
            asset.setData(file.getBytes());
        } catch (IOException ex) {
            throw ApiException.badRequest("Could not read uploaded file");
        }
        return repository.save(asset);
    }

    public MediaAsset getById(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Media asset", id));
    }
}

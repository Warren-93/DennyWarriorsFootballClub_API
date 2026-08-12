package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.exception.InvalidMediaException;
import mark.warren93.dev.DennyWarriorsAPI.exception.ResourceNotFoundException;
import mark.warren93.dev.DennyWarriorsAPI.model.MediaAsset;
import mark.warren93.dev.DennyWarriorsAPI.repository.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class MediaService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    private final MediaAssetRepository mediaAssetRepository;

    public MediaService(MediaAssetRepository mediaAssetRepository) {
        this.mediaAssetRepository = mediaAssetRepository;
    }

    public MediaAsset upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidMediaException("No file provided");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidMediaException("File exceeds the 5MB size limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidMediaException("Unsupported file type: " + contentType
                    + " (allowed: " + ALLOWED_CONTENT_TYPES + ")");
        }

        MediaAsset asset = new MediaAsset();
        asset.setFilename(file.getOriginalFilename());
        asset.setContentType(contentType);
        asset.setSize(file.getSize());
        asset.setUploadedAt(LocalDateTime.now());
        try {
            asset.setData(file.getBytes());
        } catch (IOException ex) {
            throw new InvalidMediaException("Could not read uploaded file: " + ex.getMessage());
        }
        return mediaAssetRepository.save(asset);
    }

    public MediaAsset getById(String id) {
        return mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + id));
    }
}

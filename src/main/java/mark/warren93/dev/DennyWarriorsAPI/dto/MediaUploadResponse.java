package mark.warren93.dev.DennyWarriorsAPI.dto;

import mark.warren93.dev.DennyWarriorsAPI.model.MediaAsset;

public record MediaUploadResponse(String id, String filename, String contentType, long size, String url) {

    public static MediaUploadResponse from(MediaAsset asset) {
        return new MediaUploadResponse(
                asset.getId(),
                asset.getFilename(),
                asset.getContentType(),
                asset.getSize(),
                "/api/v1/media/" + asset.getId());
    }
}

package com.clubsite.api.media;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * An uploaded image, stored as binary in MongoDB. Simple and free for a
 * club's volume of images (well under the 16MB document limit at the 5MB
 * default cap). Swap MediaService's storage for S3/R2 if that ever changes.
 */
@Data
@NoArgsConstructor
@Document(collection = "media_assets")
public class MediaAsset {

    @Id
    private String id;
    private String filename;
    private String contentType;
    private long size;
    private byte[] data;
    private Instant uploadedAt;
}

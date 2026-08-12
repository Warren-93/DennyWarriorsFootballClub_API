package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "media_assets")
public class MediaAsset {

    @Id
    private String id;
    private String filename;
    private String contentType;
    private long size;
    private byte[] data;
    private LocalDateTime uploadedAt;
}

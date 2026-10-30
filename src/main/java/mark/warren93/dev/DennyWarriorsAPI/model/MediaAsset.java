package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
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

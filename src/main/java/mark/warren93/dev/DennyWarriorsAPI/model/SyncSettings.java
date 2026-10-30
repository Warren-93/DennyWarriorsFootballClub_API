package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Singleton document (fixed id) holding the live-adjustable league sync
 * interval, so it survives restarts and can be changed from the admin panel
 * without a redeploy.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
@Document(collection = "sync_settings")
public class SyncSettings {

    public static final String SINGLETON_ID = "singleton";

    @Id
    private String id;
    private long intervalMs;
    private Instant updatedAt;
}

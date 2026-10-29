package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Singleton document (fixed id) holding the live-adjustable league sync
 * settings — the interval and the Comet report links — so they survive
 * restarts and can be changed from the admin panel without a redeploy.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sync_settings")
public class SyncSettings {

    public static final String SINGLETON_ID = "singleton";

    @Id
    private String id;
    private long intervalMs;

    /**
     * Comet report links pasted in the admin panel. Each one carries an
     * API_KEY, so they are never returned by the API (see CometLinkService).
     * Null means "fall back to the COMET_*_URL environment variable".
     */
    private String cometFixturesUrl;
    private String cometStandingsUrl;

    private Instant updatedAt;
}

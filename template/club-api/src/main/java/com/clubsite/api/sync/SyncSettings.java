package com.clubsite.api.sync;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Singleton document holding the live sync interval, so an admin can change
 * it without a redeploy and the change survives restarts.
 */
@Data
@NoArgsConstructor
@Document(collection = "sync_settings")
public class SyncSettings {

    public static final String SINGLETON_ID = "singleton";

    @Id
    private String id;
    private long intervalMs;
    private Instant updatedAt;
}

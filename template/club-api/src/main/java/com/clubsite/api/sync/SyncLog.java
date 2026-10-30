package com.clubsite.api.sync;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@Document(collection = "sync_logs")
public class SyncLog {

    public enum Status { SUCCESS, FAILED, SKIPPED }

    public enum Trigger { SCHEDULED, MANUAL }

    @Id
    private String id;
    @Indexed
    private Instant startedAt;
    private Instant finishedAt;
    private String provider;
    private Status status;
    private Trigger trigger;
    private int recordsProcessed;
    private int recordsUpserted;
    private String message;
}

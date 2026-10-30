package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Document(collection = "sync_logs")
public class SyncLog {

    @Id
    private String id;
    private Instant startedAt;
    private Instant finishedAt;
    private String status; // SUCCESS | PARTIAL | FAILED
    private int recordsProcessed;
    private int recordsUpserted;
    private String errorMessage;
    private String trigger; // SCHEDULED | MANUAL
}

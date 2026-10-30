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
@Document(collection = "history_entries")
public class HistoryEntry {

    @Id
    private String id;
    private HistoryEntryType type;
    private Integer year;
    private String title;
    private String description;
    private String imageUrl;
    private int order;
    private LocalDateTime createdAt;
}

package com.clubsite.api.history;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A club-history timeline milestone, or an honour (trophy, promotion...). */
@Data
@NoArgsConstructor
@Document(collection = "history_entries")
public class HistoryEntry {

    public enum Type { TIMELINE, HONOUR }

    @Id
    private String id;
    private Type type;
    private Integer year;
    private String title;
    private String description;
    private String imageUrl;
    /** Manual ordering within the same type; ties fall back to year. */
    private int displayOrder;
    private Instant createdAt;
    private Instant updatedAt;
}

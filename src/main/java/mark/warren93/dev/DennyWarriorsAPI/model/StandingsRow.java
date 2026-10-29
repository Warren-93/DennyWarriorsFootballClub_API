package mark.warren93.dev.DennyWarriorsAPI.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "standings")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StandingsRow {

    /**
     * Mongo's persistence id. The Comet JSON also has a numeric "id" column
     * (the competition's id, see {@link #competitionId}) — @JsonIgnore keeps
     * that from clobbering this field when deserializing the raw API response.
     */
    @Id
    @JsonIgnore
    private String id;

    /**
     * Comet's "id" column: the competition this table row belongs to. A new
     * season is a new competition, so this (with {@link #status}) is how sync
     * picks the right table and spots a finished one.
     */
    @JsonProperty("id")
    private Long competitionId;
    @JsonProperty("name")
    private String competition;   // e.g. "Appin Warriors Premier League 25/26"
    private String season;        // e.g. "2025/2026"
    private String status;        // competition status: ACTIVE | INACTIVE | ...
    private Long clubId;
    private Integer position;
    private String club;
    private Integer matches;
    private Integer wins;
    private Integer draws;
    private Integer losses;
    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer goalDifference;
    private Integer points;
}

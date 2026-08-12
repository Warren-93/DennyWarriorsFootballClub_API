package mark.warren93.dev.DennyWarriorsAPI.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
     * Mongo's persistence id. The Comet JSON also has an unrelated numeric
     * "id" column (a report-row id) — @JsonIgnore keeps that from clobbering
     * this field when deserializing the raw API response.
     */
    @Id
    @JsonIgnore
    private String id;
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

package mark.warren93.dev.DennyWarriorsAPI.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchResult {

    private Long matchId;
    private String matchDescription;
    private String matchStatus;
    private String liveStatus;
    private Long matchDate;
    private Integer round;
    private Integer matchNumber;
    private String name;           // Competition name
    private String competitionType;
    private String season;
    private Long homeTeam;
    private Long awayTeam;
    private String homeTeamStatus;
    private String awayTeamStatus;
    private String facility;
    private String facilityPlaceName;
    private String country;
    private String resultSupplement; // e.g. "Ineligible players forced forfeit"
    private String gender;
    private String category;
}

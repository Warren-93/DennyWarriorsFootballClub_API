package mark.warren93.dev.DennyWarriorsAPI.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import mark.warren93.dev.DennyWarriorsAPI.model.StandingsRow;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StandingsResponse {
    private List<StandingsRow> results;
}

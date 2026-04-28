package mark.warren93.dev.DennyWarriorsAPI.dto;

import mark.warren93.dev.DennyWarriorsAPI.model.LeagueTableRow;

import java.util.List;

public record LeagueTableUpdateRequest(List<LeagueTableRow> rows) {

}

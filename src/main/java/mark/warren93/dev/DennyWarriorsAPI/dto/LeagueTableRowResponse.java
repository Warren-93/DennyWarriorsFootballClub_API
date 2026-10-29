package mark.warren93.dev.DennyWarriorsAPI.dto;

import mark.warren93.dev.DennyWarriorsAPI.config.ClubConfig;
import mark.warren93.dev.DennyWarriorsAPI.model.StandingsRow;

public record LeagueTableRowResponse(
        Integer pos,
        String team,
        Integer played,
        Integer won,
        Integer drawn,
        Integer lost,
        Integer gf,
        Integer ga,
        Integer gd,
        Integer points,
        boolean isClub,
        String competition,
        String season) {

    public static LeagueTableRowResponse from(StandingsRow row, ClubConfig club) {
        return new LeagueTableRowResponse(
                row.getPosition(),
                row.getClub(),
                row.getMatches(),
                row.getWins(),
                row.getDraws(),
                row.getLosses(),
                row.getGoalsFor(),
                row.getGoalsAgainst(),
                row.getGoalDifference(),
                row.getPoints(),
                club.isClub(row.getClubId(), row.getClub()),
                row.getCompetition(),
                row.getSeason());
    }
}

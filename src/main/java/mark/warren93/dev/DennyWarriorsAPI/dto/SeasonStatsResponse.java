package mark.warren93.dev.DennyWarriorsAPI.dto;

public record SeasonStatsResponse(
        String season,
        int played,
        int won,
        int drawn,
        int lost,
        int goalsFor,
        int goalsAgainst,
        int points,
        Integer leaguePosition) {
}

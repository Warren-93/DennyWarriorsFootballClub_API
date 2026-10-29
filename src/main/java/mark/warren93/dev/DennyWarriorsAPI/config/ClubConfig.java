package mark.warren93.dev.DennyWarriorsAPI.config;

import lombok.Data;
import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * How the club appears in Comet's data, used to derive club-relative fields
 * (opponent, home/away, goalsFor/goalsAgainst, league position) from the
 * neutral home/away Fixture and standings data synced from Comet.
 */
@Configuration
@ConfigurationProperties(prefix = "dwfc.club")
@Data
public class ClubConfig {

    /** The club's name exactly as Comet spells it (matched case-insensitively). */
    private String name;

    /**
     * The club's Comet club id (the standings report's clubId column, and the
     * fixtures report's homeTeam/awayTeam). Unlike the name it survives a
     * rename, so matching prefers it whenever the data carries an id.
     */
    private Long cometClubId;

    public boolean isClubName(String candidate) {
        return name != null && candidate != null && name.trim().equalsIgnoreCase(candidate.trim());
    }

    public boolean isHomeTeam(Fixture fixture) {
        return isClub(fixture.getHomeTeamId(), fixture.getHomeTeam());
    }

    public boolean isAwayTeam(Fixture fixture) {
        return isClub(fixture.getAwayTeamId(), fixture.getAwayTeam());
    }

    public boolean isClub(Long candidateClubId, String candidateName) {
        if (cometClubId != null && candidateClubId != null) {
            return cometClubId.equals(candidateClubId);
        }
        return isClubName(candidateName);
    }
}

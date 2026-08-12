package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "players")
public class Player {

    @Id
    private String id;
    private Integer playerId;
    private Integer playerNumber;
    private String playerFirstName;
    private String playerSurename;
    private int playerAge;
    private String playerNationality;
    private String playerProfileImage;
    private String playerInfoCard;
    private String position;
    private Integer goals;
    private Integer assists;
    private Integer appearances;
    private String bio;
    private boolean captain;
    // Up to 3 general sponsor logos for this player's card — no fixed
    // category (not "home/away/boot"), just optional slots that only
    // render on the public card when populated.
    private String sponsorLogo1;
    private String sponsorLogo2;
    private String sponsorLogo3;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

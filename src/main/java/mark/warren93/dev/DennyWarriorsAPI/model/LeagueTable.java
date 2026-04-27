package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "league_table")
public class LeagueTable {

    @Id
    private String id;
    private String key;
    private String leagueName;
    private String season;
    private List<LeagueTableRow> rows = new ArrayList<>();
    private LocalDateTime updatedAt;
}

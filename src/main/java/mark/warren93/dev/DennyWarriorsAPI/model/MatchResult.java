package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "results")
public class MatchResult {

    @Id
    private String id;
    private String fixtureId;
    private String opponent;
    private String competition;
    private String season;
    private String venue;
    private LocalDate matchDate;
    private int dennyWarriorsScore;
    private int opponentScore;
    private List<String> scorers = new ArrayList<>();
    private String report;
    private boolean home;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

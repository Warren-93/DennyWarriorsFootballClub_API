package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fixtures")
public class Fixture {

    @Id
    private String id;
    private String opponent;
    private String competition;
    private String season;
    private String venue;
    private LocalDate fixtureDate;
    private String kickoffTime;
    private boolean home;
    private boolean played;
    private String status;
    private String notes;
    private String ticketUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

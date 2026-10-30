package mark.warren93.dev.DennyWarriorsAPI.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Squad {
    private List<Player> squadList;

}

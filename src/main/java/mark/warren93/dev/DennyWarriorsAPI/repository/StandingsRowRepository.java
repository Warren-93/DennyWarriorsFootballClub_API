package mark.warren93.dev.DennyWarriorsAPI.repository;

import mark.warren93.dev.DennyWarriorsAPI.model.StandingsRow;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StandingsRowRepository extends MongoRepository<StandingsRow, String> {
}

package mark.warren93.dev.DennyWarriorsAPI.repository;

import mark.warren93.dev.DennyWarriorsAPI.model.LeagueTable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LeagueTableRepository extends MongoRepository<LeagueTable, String> {

    Optional<LeagueTable> findByKey(String key);
}

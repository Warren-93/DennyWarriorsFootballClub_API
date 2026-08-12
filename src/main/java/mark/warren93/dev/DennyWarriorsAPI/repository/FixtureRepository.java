package mark.warren93.dev.DennyWarriorsAPI.repository;

import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FixtureRepository extends MongoRepository<Fixture, String> {

    Optional<Fixture> findByLeagueFixtureId(String leagueFixtureId);
}

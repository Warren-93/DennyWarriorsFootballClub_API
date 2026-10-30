package com.clubsite.api.matches;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Optional;

public interface FixtureRepository extends MongoRepository<Fixture, String> {

    Optional<Fixture> findByExternalId(String externalId);

    Optional<Fixture> findFirstByKickoffAtAfterOrderByKickoffAtAsc(Instant after);
}

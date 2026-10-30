package com.clubsite.api.matches;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StandingsRowRepository extends MongoRepository<StandingsRow, String> {

    List<StandingsRow> findAllByOrderByPositionAsc();
}

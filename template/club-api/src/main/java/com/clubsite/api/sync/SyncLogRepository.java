package com.clubsite.api.sync;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SyncLogRepository extends MongoRepository<SyncLog, String> {

    List<SyncLog> findTop50ByOrderByStartedAtDesc();
}

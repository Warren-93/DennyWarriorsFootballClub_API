package com.clubsite.api.sync;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SyncSettingsRepository extends MongoRepository<SyncSettings, String> {
}

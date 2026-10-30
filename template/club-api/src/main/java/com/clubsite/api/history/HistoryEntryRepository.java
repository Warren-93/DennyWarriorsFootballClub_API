package com.clubsite.api.history;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface HistoryEntryRepository extends MongoRepository<HistoryEntry, String> {
}

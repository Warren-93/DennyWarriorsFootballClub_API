package mark.warren93.dev.DennyWarriorsAPI.repository;

import mark.warren93.dev.DennyWarriorsAPI.model.HistoryEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HistoryEntryRepository extends MongoRepository<HistoryEntry, String> {
}

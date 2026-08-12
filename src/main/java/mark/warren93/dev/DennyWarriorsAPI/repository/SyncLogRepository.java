package mark.warren93.dev.DennyWarriorsAPI.repository;

import mark.warren93.dev.DennyWarriorsAPI.model.SyncLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SyncLogRepository extends MongoRepository<SyncLog, String> {
}

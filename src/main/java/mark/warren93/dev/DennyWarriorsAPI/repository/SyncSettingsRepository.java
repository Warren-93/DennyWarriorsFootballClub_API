package mark.warren93.dev.DennyWarriorsAPI.repository;

import mark.warren93.dev.DennyWarriorsAPI.model.SyncSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SyncSettingsRepository extends MongoRepository<SyncSettings, String> {
}

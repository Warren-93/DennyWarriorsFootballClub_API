package mark.warren93.dev.DennyWarriorsAPI.repository;

import mark.warren93.dev.DennyWarriorsAPI.model.SyncSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SyncSettingsRepository extends MongoRepository<SyncSettings, String> {

    /** The singleton settings document, or a fresh unsaved one if none exists yet. */
    default SyncSettings loadOrCreate() {
        return findById(SyncSettings.SINGLETON_ID).orElseGet(() -> {
            SyncSettings fresh = new SyncSettings();
            fresh.setId(SyncSettings.SINGLETON_ID);
            return fresh;
        });
    }
}

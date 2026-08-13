package mark.warren93.dev.DennyWarriorsAPI.dto;

public record SyncSettingsResponse(long intervalMs, long intervalDays) {
    public static SyncSettingsResponse of(long intervalMs) {
        return new SyncSettingsResponse(intervalMs, intervalMs / 86_400_000);
    }
}

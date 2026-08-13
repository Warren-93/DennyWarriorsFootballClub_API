package mark.warren93.dev.DennyWarriorsAPI.dto;

public record SyncSettingsResponse(long intervalMs, long intervalMinutes) {
    public static SyncSettingsResponse of(long intervalMs) {
        return new SyncSettingsResponse(intervalMs, intervalMs / 60_000);
    }
}

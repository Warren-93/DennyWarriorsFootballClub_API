package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.dto.ApiListResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.SyncSettingsResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.UpdateSyncSettingsRequest;
import mark.warren93.dev.DennyWarriorsAPI.model.SyncLog;
import mark.warren93.dev.DennyWarriorsAPI.repository.SyncLogRepository;
import mark.warren93.dev.DennyWarriorsAPI.service.SyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;

@RestController
@RequestMapping("/api/v1/admin/sync")
public class AdminSyncController {

    private final SyncService syncService;
    private final SyncLogRepository syncLogRepository;

    public AdminSyncController(SyncService syncService, SyncLogRepository syncLogRepository) {
        this.syncService = syncService;
        this.syncLogRepository = syncLogRepository;
    }

    @PostMapping("/trigger")
    public SyncLog triggerSync() {
        return syncService.sync(SyncService.TRIGGER_MANUAL);
    }

    @GetMapping("/logs")
    public ApiListResponse<SyncLog> getSyncLogs() {
        var logs = syncLogRepository.findAll().stream()
                .sorted(Comparator.comparing(SyncLog::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return ApiListResponse.of(logs);
    }

    @GetMapping("/settings")
    public SyncSettingsResponse getSettings() {
        return SyncSettingsResponse.of(syncService.getIntervalMs());
    }

    @PutMapping("/settings")
    public SyncSettingsResponse updateSettings(@RequestBody UpdateSyncSettingsRequest request) {
        long intervalMs = request.intervalDays() * 86_400_000L;
        return SyncSettingsResponse.of(syncService.updateIntervalMs(intervalMs).getIntervalMs());
    }
}

package com.clubsite.api.sync;

import com.clubsite.api.common.ApiListResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/admin/sync")
public class AdminSyncController {

    public record UpdateSyncSettingsRequest(@Min(1) long intervalDays) {
    }

    private final SyncService syncService;

    public AdminSyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/trigger")
    public SyncLog trigger() {
        return syncService.sync(SyncLog.Trigger.MANUAL);
    }

    @GetMapping("/logs")
    public ApiListResponse<SyncLog> logs() {
        return ApiListResponse.of(syncService.recentLogs());
    }

    @GetMapping("/settings")
    public SyncService.SyncStatus settings() {
        return syncService.status();
    }

    @PutMapping("/settings")
    public SyncService.SyncStatus updateSettings(@Valid @RequestBody UpdateSyncSettingsRequest request) {
        return syncService.updateInterval(Duration.ofDays(request.intervalDays()));
    }
}

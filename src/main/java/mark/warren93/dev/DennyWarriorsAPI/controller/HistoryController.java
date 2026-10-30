package mark.warren93.dev.DennyWarriorsAPI.controller;

import lombok.RequiredArgsConstructor;
import mark.warren93.dev.DennyWarriorsAPI.dto.ApiListResponse;
import mark.warren93.dev.DennyWarriorsAPI.model.HistoryEntry;
import mark.warren93.dev.DennyWarriorsAPI.model.HistoryEntryType;
import mark.warren93.dev.DennyWarriorsAPI.service.HistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    public ApiListResponse<HistoryEntry> getHistory(@RequestParam(required = false) HistoryEntryType type) {
        return ApiListResponse.of(historyService.getEntries(type));
    }
}

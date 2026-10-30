package mark.warren93.dev.DennyWarriorsAPI.controller;

import lombok.RequiredArgsConstructor;
import mark.warren93.dev.DennyWarriorsAPI.dto.SeasonStatsResponse;
import mark.warren93.dev.DennyWarriorsAPI.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public SeasonStatsResponse getSeasonStats() {
        return statsService.getSeasonStats();
    }
}

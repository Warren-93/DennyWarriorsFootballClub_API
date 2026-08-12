package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.dto.ApiListResponse;
import mark.warren93.dev.DennyWarriorsAPI.model.StandingsRow;
import mark.warren93.dev.DennyWarriorsAPI.repository.StandingsRowRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;

@RestController
@RequestMapping("/api/v1/standings")
public class StandingsController {

    private final StandingsRowRepository standingsRowRepository;

    public StandingsController(StandingsRowRepository standingsRowRepository) {
        this.standingsRowRepository = standingsRowRepository;
    }

    @GetMapping
    public ApiListResponse<StandingsRow> getStandings() {
        var rows = standingsRowRepository.findAll().stream()
                .sorted(Comparator.comparing(StandingsRow::getPosition, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        return ApiListResponse.of(rows);
    }
}

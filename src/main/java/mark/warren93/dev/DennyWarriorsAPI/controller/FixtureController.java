package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.dto.ApiListResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.FixtureResponse;
import mark.warren93.dev.DennyWarriorsAPI.service.FixtureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fixtures")
public class FixtureController {

    private final FixtureService fixtureService;

    public FixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @GetMapping
    public ApiListResponse<FixtureResponse> getFixtures(
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String competition,
            @RequestParam(required = false) String status) {
        var fixtures = fixtureService.getFixtures(season, competition, status).stream()
                .map(FixtureResponse::from)
                .toList();
        return ApiListResponse.of(fixtures);
    }

    @GetMapping("/{id}")
    public FixtureResponse getFixtureById(@PathVariable String id) {
        return FixtureResponse.from(fixtureService.getFixtureById(id));
    }
}

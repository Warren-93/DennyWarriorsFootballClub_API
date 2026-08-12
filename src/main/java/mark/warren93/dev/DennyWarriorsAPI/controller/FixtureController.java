package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.config.ClubConfig;
import mark.warren93.dev.DennyWarriorsAPI.dto.ApiListResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.FixtureResponse;
import mark.warren93.dev.DennyWarriorsAPI.service.FixtureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fixtures")
public class FixtureController {

    private final FixtureService fixtureService;
    private final ClubConfig clubConfig;

    public FixtureController(FixtureService fixtureService, ClubConfig clubConfig) {
        this.fixtureService = fixtureService;
        this.clubConfig = clubConfig;
    }

    @GetMapping
    public ApiListResponse<FixtureResponse> getFixtures(
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String competition,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        var fixtures = fixtureService.getFixtures(season, competition, status, limit).stream()
                .map(fixture -> FixtureResponse.from(fixture, clubConfig.getName()))
                .toList();
        return ApiListResponse.of(fixtures);
    }

    @GetMapping("/seasons")
    public List<String> getSeasons() {
        return fixtureService.getSeasons();
    }

    @GetMapping("/competitions")
    public List<String> getCompetitions() {
        return fixtureService.getCompetitions();
    }

    @GetMapping("/next")
    public ResponseEntity<FixtureResponse> getNextFixture() {
        return fixtureService.getNextFixture()
                .map(fixture -> ResponseEntity.ok(FixtureResponse.from(fixture, clubConfig.getName())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public FixtureResponse getFixtureById(@PathVariable String id) {
        return FixtureResponse.from(fixtureService.getFixtureById(id), clubConfig.getName());
    }
}

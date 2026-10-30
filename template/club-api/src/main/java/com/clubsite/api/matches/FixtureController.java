package com.clubsite.api.matches;

import com.clubsite.api.common.ApiListResponse;
import com.clubsite.api.config.ClubProperties;
import com.clubsite.api.matches.FixtureDtos.FixtureRequest;
import com.clubsite.api.matches.FixtureDtos.FixtureResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.List;

/**
 * Fixtures and results are the same resource: results are
 * GET /fixtures?status=past, upcoming matches GET /fixtures?status=upcoming.
 */
@RestController
@RequestMapping("/api/v1")
public class FixtureController {

    private final FixtureService fixtureService;
    private final ClubPerspective perspective;
    private final ZoneId zone;

    public FixtureController(FixtureService fixtureService, ClubPerspective perspective, ClubProperties properties) {
        this.fixtureService = fixtureService;
        this.perspective = perspective;
        this.zone = properties.timezone();
    }

    // --- Public ------------------------------------------------------------

    @GetMapping({"/fixtures", "/admin/fixtures"})
    public ApiListResponse<FixtureResponse> list(
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String competition,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return ApiListResponse.of(fixtureService.list(season, competition, status, limit).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/fixtures/next")
    public ResponseEntity<FixtureResponse> next() {
        return fixtureService.next()
                .map(f -> ResponseEntity.ok(toResponse(f)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/fixtures/seasons")
    public List<String> seasons() {
        return fixtureService.seasons();
    }

    @GetMapping("/fixtures/competitions")
    public List<String> competitions() {
        return fixtureService.competitions();
    }

    @GetMapping("/fixtures/{id}")
    public FixtureResponse get(@PathVariable String id) {
        return toResponse(fixtureService.getById(id));
    }

    // --- Admin -------------------------------------------------------------

    @PostMapping("/admin/fixtures")
    @ResponseStatus(HttpStatus.CREATED)
    public FixtureResponse create(@Valid @RequestBody FixtureRequest request) {
        return toResponse(fixtureService.create(request));
    }

    @PutMapping("/admin/fixtures/{id}")
    public FixtureResponse update(@PathVariable String id, @Valid @RequestBody FixtureRequest request) {
        return toResponse(fixtureService.update(id, request));
    }

    @DeleteMapping("/admin/fixtures/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        fixtureService.delete(id);
    }

    private FixtureResponse toResponse(Fixture fixture) {
        return FixtureResponse.from(fixture, perspective, zone);
    }
}

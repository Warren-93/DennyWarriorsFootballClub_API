package mark.warren93.dev.DennyWarriorsAPI.controller;

import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import mark.warren93.dev.DennyWarriorsAPI.service.FixtureService;
import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping("/fixtures")
public class FixtureController {

    private final FixtureService fixtureService;

    public FixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @GetMapping
    public List<Fixture> getFixtures(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String competition,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) Boolean played) {
        return fixtureService.getFixtures(limit, competition, season, played);
    }

    @GetMapping("/{id}")
    public Fixture getFixtureById(@PathVariable String id) {
        return fixtureService.getFixtureById(id);
    }

    @GetMapping("/next")
    public Fixture getNextFixture() {
        return fixtureService.getNextFixture();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Fixture createFixture(@RequestBody Fixture fixture) {
        return fixtureService.createFixture(fixture);
    }

    @PutMapping("/{id}")
    public Fixture updateFixture(@PathVariable String id, @RequestBody Fixture fixture) {
        return fixtureService.updateFixture(id, fixture);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFixture(@PathVariable String id) {
        fixtureService.deleteFixture(id);
    }
}

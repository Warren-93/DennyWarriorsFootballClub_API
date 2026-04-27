package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.exception.ResourceNotFoundException;
import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import mark.warren93.dev.DennyWarriorsAPI.repository.FixtureRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class FixtureService {

    private final FixtureRepository fixtureRepository;

    public FixtureService(FixtureRepository fixtureRepository) {
        this.fixtureRepository = fixtureRepository;
    }

    public List<Fixture> getFixtures(Integer limit, String competition, String season, Boolean played) {
        Stream<Fixture> stream = fixtureRepository.findAll().stream()
                .filter(fixture -> isBlank(competition) || equalsIgnoreCase(fixture.getCompetition(), competition))
                .filter(fixture -> isBlank(season) || equalsIgnoreCase(fixture.getSeason(), season))
                .filter(fixture -> played == null || fixture.isPlayed() == played)
                .sorted(Comparator.comparing(Fixture::getFixtureDate, Comparator.nullsLast(LocalDate::compareTo)));

        if (limit != null && limit > 0) {
            stream = stream.limit(limit);
        }

        return stream.toList();
    }

    public Fixture getFixtureById(String id) {
        return fixtureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fixture not found: " + id));
    }

    public Fixture getNextFixture() {
        return fixtureRepository.findAll().stream()
                .filter(fixture -> !fixture.isPlayed())
                .filter(fixture -> fixture.getFixtureDate() == null || !fixture.getFixtureDate().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(Fixture::getFixtureDate, Comparator.nullsLast(LocalDate::compareTo)))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No upcoming fixture found"));
    }

    public Fixture createFixture(Fixture fixture) {
        LocalDateTime now = LocalDateTime.now();
        fixture.setId(null);
        fixture.setCreatedAt(now);
        fixture.setUpdatedAt(now);
        if (isBlank(fixture.getStatus())) {
            fixture.setStatus("scheduled");
        }
        return fixtureRepository.save(fixture);
    }

    public Fixture updateFixture(String id, Fixture fixture) {
        Fixture existing = getFixtureById(id);
        existing.setOpponent(fixture.getOpponent());
        existing.setCompetition(fixture.getCompetition());
        existing.setSeason(fixture.getSeason());
        existing.setVenue(fixture.getVenue());
        existing.setFixtureDate(fixture.getFixtureDate());
        existing.setKickoffTime(fixture.getKickoffTime());
        existing.setHome(fixture.isHome());
        existing.setPlayed(fixture.isPlayed());
        existing.setStatus(fixture.getStatus());
        existing.setNotes(fixture.getNotes());
        existing.setTicketUrl(fixture.getTicketUrl());
        existing.setUpdatedAt(LocalDateTime.now());
        return fixtureRepository.save(existing);
    }

    public void deleteFixture(String id) {
        Fixture existing = getFixtureById(id);
        fixtureRepository.delete(existing);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return Objects.equals(normalise(left), normalise(right));
    }

    private String normalise(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}

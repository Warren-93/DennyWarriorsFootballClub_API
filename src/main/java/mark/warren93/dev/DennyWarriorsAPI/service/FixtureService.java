package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.exception.ResourceNotFoundException;
import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import mark.warren93.dev.DennyWarriorsAPI.repository.FixtureRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public List<Fixture> getFixtures(String season, String competition, String status) {
        Stream<Fixture> stream = fixtureRepository.findAll().stream()
                .filter(fixture -> isBlank(season) || equalsIgnoreCase(fixture.getSeason(), season))
                .filter(fixture -> isBlank(competition) || equalsIgnoreCase(fixture.getCompetition(), competition))
                .filter(fixture -> matchesStatus(fixture, status))
                .sorted(Comparator.comparing(Fixture::getKickoffAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return stream.toList();
    }

    public Fixture getFixtureById(String id) {
        return fixtureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fixture not found: " + id));
    }

    /**
     * "status" accepts the spec's semantic "upcoming"/"past" (judged by
     * kickoff time, since that's independent of whatever status vocabulary
     * Comet happens to use), or an exact match against the raw league status
     * (e.g. "PLAYED", "SCHEDULED") for callers that want that instead.
     */
    private boolean matchesStatus(Fixture fixture, String status) {
        if (isBlank(status)) {
            return true;
        }
        String normalised = normalise(status);
        if ("upcoming".equals(normalised)) {
            return fixture.getKickoffAt() != null && fixture.getKickoffAt().isAfter(Instant.now());
        }
        if ("past".equals(normalised)) {
            return fixture.getKickoffAt() != null && !fixture.getKickoffAt().isAfter(Instant.now());
        }
        return equalsIgnoreCase(fixture.getStatus(), status);
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

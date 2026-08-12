package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.exception.ResourceNotFoundException;
import mark.warren93.dev.DennyWarriorsAPI.model.Fixture;
import mark.warren93.dev.DennyWarriorsAPI.repository.FixtureRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class FixtureService {

    private final FixtureRepository fixtureRepository;

    public FixtureService(FixtureRepository fixtureRepository) {
        this.fixtureRepository = fixtureRepository;
    }

    public List<Fixture> getFixtures(String season, String competition, String status, Integer limit) {
        Stream<Fixture> stream = fixtureRepository.findAll().stream()
                .filter(fixture -> isBlank(season) || equalsIgnoreCase(fixture.getSeason(), season))
                .filter(fixture -> isBlank(competition) || equalsIgnoreCase(fixture.getCompetition(), competition))
                .filter(fixture -> matchesStatus(fixture, status))
                .sorted(statusOrderedComparator(status));

        if (limit != null && limit > 0) {
            stream = stream.limit(limit);
        }
        return stream.toList();
    }

    public Fixture getFixtureById(String id) {
        return fixtureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fixture not found: " + id));
    }

    /** The single earliest not-yet-played fixture, if any. */
    public Optional<Fixture> getNextFixture() {
        Instant now = Instant.now();
        return fixtureRepository.findAll().stream()
                .filter(fixture -> fixture.getKickoffAt() != null && fixture.getKickoffAt().isAfter(now))
                .min(Comparator.comparing(Fixture::getKickoffAt));
    }

    /**
     * Distinct seasons present in the synced fixtures, newest first — lets the
     * frontend populate a season filter without hardcoding season strings.
     * Each new season comes from a new Comet report URL (COMET_FIXTURES_URL),
     * but old seasons' fixtures stay in Mongo (upserted, never deleted), so
     * this naturally grows as the club moves through seasons.
     */
    public List<String> getSeasons() {
        return fixtureRepository.findAll().stream()
                .map(Fixture::getSeason)
                .filter(season -> season != null && !season.isBlank())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /** Distinct competition names present in the synced fixtures, alphabetical. */
    public List<String> getCompetitions() {
        return fixtureRepository.findAll().stream()
                .map(Fixture::getCompetition)
                .filter(competition -> competition != null && !competition.isBlank())
                .distinct()
                .sorted()
                .toList();
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

    /** "past"/results reads most-recent-first; everything else stays chronological. */
    private Comparator<Fixture> statusOrderedComparator(String status) {
        Comparator<Fixture> chronological =
                Comparator.comparing(Fixture::getKickoffAt, Comparator.nullsLast(Comparator.naturalOrder()));
        return "past".equals(normalise(status)) ? chronological.reversed() : chronological;
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

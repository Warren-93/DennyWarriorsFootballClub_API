package com.clubsite.api.matches;

import com.clubsite.api.common.ApiException;
import com.clubsite.api.common.Text;
import com.clubsite.api.matches.FixtureDtos.FixtureRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class FixtureService {

    /** Status filter values with a meaning of their own, judged by kick-off time. */
    public static final String UPCOMING = "upcoming";
    public static final String PAST = "past";

    private final FixtureRepository repository;
    private final MongoTemplate mongoTemplate;

    public FixtureService(FixtureRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * @param status "upcoming", "past", or an exact (case-insensitive) match on
     *               the raw league status such as "PLAYED". Kick-off time decides
     *               upcoming/past because status vocabularies differ per league.
     *               "past" is newest-first; everything else is chronological.
     */
    public List<Fixture> list(String season, String competition, String status, Integer limit) {
        Query query = new Query();
        if (!Text.isBlank(season)) {
            query.addCriteria(Criteria.where("season").regex(exactIgnoreCase(season)));
        }
        if (!Text.isBlank(competition)) {
            query.addCriteria(Criteria.where("competition").regex(exactIgnoreCase(competition)));
        }

        String normalisedStatus = Text.normalise(status);
        Instant now = Instant.now();
        if (UPCOMING.equals(normalisedStatus)) {
            query.addCriteria(Criteria.where("kickoffAt").gt(now));
        } else if (PAST.equals(normalisedStatus)) {
            query.addCriteria(Criteria.where("kickoffAt").lte(now));
        } else if (!Text.isBlank(status)) {
            query.addCriteria(Criteria.where("status").regex(exactIgnoreCase(status)));
        }

        query.with(PAST.equals(normalisedStatus) ? Sort.by(Sort.Direction.DESC, "kickoffAt") : Sort.by("kickoffAt"));
        if (limit != null && limit > 0) {
            query.limit(limit);
        }
        return mongoTemplate.find(query, Fixture.class);
    }

    public Fixture getById(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Fixture", id));
    }

    public Optional<Fixture> next() {
        return repository.findFirstByKickoffAtAfterOrderByKickoffAtAsc(Instant.now());
    }

    /** Distinct seasons, newest first, so the client never hardcodes "2024/25". */
    public List<String> seasons() {
        return distinct("season", Comparator.reverseOrder());
    }

    public List<String> competitions() {
        return distinct("competition", Comparator.naturalOrder());
    }

    private List<String> distinct(String field, Comparator<String> order) {
        return mongoTemplate.findDistinct(new Query(), field, Fixture.class, String.class).stream()
                .filter(value -> !Text.isBlank(value))
                .sorted(order)
                .toList();
    }

    public Fixture create(FixtureRequest request) {
        Fixture fixture = new Fixture();
        fixture.setSource(Fixture.Source.MANUAL);
        fixture.setCreatedAt(Instant.now());
        return save(fixture, request);
    }

    public Fixture update(String id, FixtureRequest request) {
        return save(getById(id), request);
    }

    public void delete(String id) {
        repository.delete(getById(id));
    }

    private Fixture save(Fixture fixture, FixtureRequest r) {
        if ((r.homeScore() == null) != (r.awayScore() == null)) {
            throw ApiException.badRequest("Set both scores, or neither for a match not yet played");
        }
        fixture.setCompetition(r.competition().trim());
        fixture.setSeason(r.season());
        fixture.setHomeTeam(r.homeTeam().trim());
        fixture.setAwayTeam(r.awayTeam().trim());
        fixture.setKickoffAt(r.kickoffAt());
        fixture.setVenue(r.venue());
        fixture.setStatus(r.status());
        fixture.setHomeScore(r.homeScore());
        fixture.setAwayScore(r.awayScore());
        fixture.setEvents(r.events() == null ? new ArrayList<>() : new ArrayList<>(r.events()));
        fixture.setUpdatedAt(Instant.now());
        return repository.save(fixture);
    }

    private static Pattern exactIgnoreCase(String value) {
        return Pattern.compile("^" + Pattern.quote(value.trim()) + "$", Pattern.CASE_INSENSITIVE);
    }
}

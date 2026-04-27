package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.exception.ResourceNotFoundException;
import mark.warren93.dev.DennyWarriorsAPI.model.MatchResult;
import mark.warren93.dev.DennyWarriorsAPI.repository.ResultRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class ResultService {

    private final ResultRepository resultRepository;

    public ResultService(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public List<MatchResult> getResults(Integer limit, String competition, String season) {
        Stream<MatchResult> stream = resultRepository.findAll().stream()
                .filter(result -> isBlank(competition) || equalsIgnoreCase(result.getCompetition(), competition))
                .filter(result -> isBlank(season) || equalsIgnoreCase(result.getSeason(), season))
                .sorted(Comparator.comparing(MatchResult::getMatchDate, Comparator.nullsLast(LocalDate::compareTo)).reversed());

        if (limit != null && limit > 0) {
            stream = stream.limit(limit);
        }

        return stream.toList();
    }

    public MatchResult getResultById(String id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found: " + id));
    }

    public MatchResult createResult(MatchResult result) {
        LocalDateTime now = LocalDateTime.now();
        result.setId(null);
        result.setCreatedAt(now);
        result.setUpdatedAt(now);
        result.setScorers(result.getScorers() == null ? new ArrayList<>() : result.getScorers());
        return resultRepository.save(result);
    }

    public MatchResult updateResult(String id, MatchResult result) {
        MatchResult existing = getResultById(id);
        existing.setFixtureId(result.getFixtureId());
        existing.setOpponent(result.getOpponent());
        existing.setCompetition(result.getCompetition());
        existing.setSeason(result.getSeason());
        existing.setVenue(result.getVenue());
        existing.setMatchDate(result.getMatchDate());
        existing.setDennyWarriorsScore(result.getDennyWarriorsScore());
        existing.setOpponentScore(result.getOpponentScore());
        existing.setScorers(result.getScorers() == null ? new ArrayList<>() : result.getScorers());
        existing.setReport(result.getReport());
        existing.setHome(result.isHome());
        existing.setUpdatedAt(LocalDateTime.now());
        return resultRepository.save(existing);
    }

    public void deleteResult(String id) {
        MatchResult existing = getResultById(id);
        resultRepository.delete(existing);
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

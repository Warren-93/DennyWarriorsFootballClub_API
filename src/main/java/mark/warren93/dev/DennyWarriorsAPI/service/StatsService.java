package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.model.MatchResult;
import mark.warren93.dev.DennyWarriorsAPI.model.SeasonStats;
import mark.warren93.dev.DennyWarriorsAPI.model.TopScorerStat;
import mark.warren93.dev.DennyWarriorsAPI.repository.ResultRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final ResultRepository resultRepository;

    public StatsService(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public SeasonStats getSeasonStats(String season) {
        List<MatchResult> allResults = resultRepository.findAll();
        String effectiveSeason = season;

        if (effectiveSeason == null || effectiveSeason.isBlank()) {
            effectiveSeason = inferCurrentSeason(allResults).orElse("current");
        }

        final String seasonFilter = effectiveSeason;
        List<MatchResult> seasonResults = allResults.stream()
                .filter(result -> Objects.equals(normalise(result.getSeason()), normalise(seasonFilter)))
                .toList();

        int played = seasonResults.size();
        int won = (int) seasonResults.stream()
                .filter(result -> result.getDennyWarriorsScore() > result.getOpponentScore())
                .count();
        int drawn = (int) seasonResults.stream()
                .filter(result -> result.getDennyWarriorsScore() == result.getOpponentScore())
                .count();
        int lost = played - won - drawn;
        int goalsFor = seasonResults.stream().mapToInt(MatchResult::getDennyWarriorsScore).sum();
        int goalsAgainst = seasonResults.stream().mapToInt(MatchResult::getOpponentScore).sum();
        long cleanSheets = seasonResults.stream()
                .filter(result -> result.getOpponentScore() == 0)
                .count();

        Map<String, Long> scorers = seasonResults.stream()
                .flatMap(result -> safeScorers(result).stream())
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

        TopScorerStat topScorer = scorers.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new TopScorerStat(entry.getKey(), entry.getValue()))
                .orElse(new TopScorerStat("N/A", 0));

        SeasonStats stats = new SeasonStats();
        stats.setSeason(effectiveSeason);
        stats.setPlayed(played);
        stats.setWon(won);
        stats.setDrawn(drawn);
        stats.setLost(lost);
        stats.setGoalsFor(goalsFor);
        stats.setGoalsAgainst(goalsAgainst);
        stats.setGoalDifference(goalsFor - goalsAgainst);
        stats.setPoints((won * 3) + drawn);
        stats.setCleanSheets(cleanSheets);
        stats.setTopScorer(topScorer);
        stats.setUpdatedAt(LocalDateTime.now());
        return stats;
    }

    private Optional<String> inferCurrentSeason(List<MatchResult> results) {
        return results.stream()
                .filter(result -> result.getSeason() != null && !result.getSeason().isBlank())
                .sorted(Comparator.comparing(MatchResult::getMatchDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(MatchResult::getSeason)
                .findFirst();
    }

    private String normalise(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private List<String> safeScorers(MatchResult result) {
        return result.getScorers() == null ? List.of() : result.getScorers();
    }
}

package com.clubsite.api.matches;

import com.clubsite.api.common.ApiException;
import com.clubsite.api.common.ApiListResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;

/**
 * The league table is read as a whole and, for manually-run leagues,
 * replaced as a whole: editing one row in isolation leaves the table
 * inconsistent anyway. With a synced provider, SyncService overwrites it.
 */
@RestController
@RequestMapping("/api/v1")
public class StandingsController {

    public record StandingsRowRequest(
            @NotNull @Min(1) Integer position,
            @NotBlank String team,
            @NotNull @Min(0) Integer played,
            @NotNull @Min(0) Integer won,
            @NotNull @Min(0) Integer drawn,
            @NotNull @Min(0) Integer lost,
            @NotNull @Min(0) Integer scoreFor,
            @NotNull @Min(0) Integer scoreAgainst,
            @NotNull Integer points) {
    }

    public record LeagueTableRowResponse(
            Integer position,
            String team,
            Integer played,
            Integer won,
            Integer drawn,
            Integer lost,
            Integer scoreFor,
            Integer scoreAgainst,
            Integer scoreDifference,
            Integer points,
            boolean isClub) {

        static LeagueTableRowResponse from(StandingsRow row, ClubPerspective perspective) {
            return new LeagueTableRowResponse(
                    row.getPosition(), row.getTeam(), row.getPlayed(), row.getWon(), row.getDrawn(), row.getLost(),
                    row.getScoreFor(), row.getScoreAgainst(), row.getScoreDifference(), row.getPoints(),
                    perspective.isClub(row.getTeam()));
        }
    }

    private final StandingsRowRepository repository;
    private final ClubPerspective perspective;

    public StandingsController(StandingsRowRepository repository, ClubPerspective perspective) {
        this.repository = repository;
        this.perspective = perspective;
    }

    @GetMapping({"/standings", "/admin/standings"})
    public ApiListResponse<LeagueTableRowResponse> list() {
        return ApiListResponse.of(repository.findAllByOrderByPositionAsc().stream()
                .map(row -> LeagueTableRowResponse.from(row, perspective))
                .toList());
    }

    @PutMapping("/admin/standings")
    public ApiListResponse<LeagueTableRowResponse> replace(@RequestBody List<@Valid StandingsRowRequest> rows) {
        if (new HashSet<>(rows.stream().map(StandingsRowRequest::position).toList()).size() != rows.size()) {
            throw ApiException.badRequest("Each position may only appear once");
        }
        repository.deleteAll();
        repository.saveAll(rows.stream()
                .map(r -> new StandingsRow(null, r.position(), r.team().trim(), r.played(), r.won(), r.drawn(),
                        r.lost(), r.scoreFor(), r.scoreAgainst(), r.scoreFor() - r.scoreAgainst(), r.points()))
                .toList());
        return list();
    }
}

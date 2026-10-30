package com.clubsite.api.sync.comet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** The subset of Comet's report JSON that the provider reads. Unknown fields are ignored. */
final class CometPayloads {

    private CometPayloads() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MatchesResponse(List<Match> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StandingsResponse(List<StandingsRow> results) {
    }

    /**
     * Comet has no separate team-name or score fields: both are embedded in
     * matchDescription as "Home - Away H:A". See CometDescriptionParser.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Match(
            Long matchId,
            String matchDescription,
            String matchStatus,
            /* Epoch milliseconds. */
            Long matchDate,
            /* Competition name. */
            String name,
            String season,
            String facility) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StandingsRow(
            Integer position,
            String club,
            Integer matches,
            Integer wins,
            Integer draws,
            Integer losses,
            Integer goalsFor,
            Integer goalsAgainst,
            Integer goalDifference,
            Integer points) {
    }
}

package com.clubsite.api.matches;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One row of the league table. "score" rather than "goals" so the same
 * columns work for points-based sports; the frontend chooses the labels.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "standings")
public class StandingsRow {

    @Id
    private String id;
    private Integer position;
    private String team;
    private Integer played;
    private Integer won;
    private Integer drawn;
    private Integer lost;
    private Integer scoreFor;
    private Integer scoreAgainst;
    private Integer scoreDifference;
    private Integer points;
}
